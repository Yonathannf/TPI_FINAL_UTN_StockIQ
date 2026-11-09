package com.stockiq.demo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.stockiq.common.util.Fechas;

import lombok.extern.slf4j.Slf4j;

/**
 * Carga datos de demostración con 120 días de historia, para mostrar la rotación, el ABC y las alertas de
 * reposición con números realistas (etapa 5). Solo se activa con el perfil {@code demo} y solo si todavía no
 * hay ningún movimiento: volver a arrancar con el perfil no duplica nada.
 *
 * Los movimientos se insertan directamente en la base con su fecha en el pasado, porque la API asigna
 * siempre la fecha actual (RN-41, D-16). La simulación respeta las reglas de negocio: nunca deja stock
 * negativo, las transferencias descuentan del origen al crearse y suman al destino al confirmarse, y los
 * productos no fraccionables se mueven en cantidades enteras.
 */
@Slf4j
@Component
@Profile("demo")
public class DatosDemoSeeder {

    static final int DIAS_DE_HISTORIA = 120;
    static final String CLAVE_OPERARIOS = "demo1234";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Random azar = new Random(42); // semilla fija: siempre genera los mismos datos

    public DatosDemoSeeder(JdbcTemplate jdbc, TransactionTemplate tx, PasswordEncoder passwordEncoder, Clock clock) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** Corre cuando la aplicación terminó de arrancar, después de crear al ENCARGADO inicial. */
    @EventListener(ApplicationReadyEvent.class)
    public void cargar() {
        Integer movimientos = jdbc.queryForObject("SELECT COUNT(*) FROM movimiento", Integer.class);
        if (movimientos != null && movimientos > 0) {
            log.info("Perfil demo: ya hay movimientos cargados, no se generan datos de demostración.");
            return;
        }
        tx.executeWithoutResult(estado -> generar());
    }

    // ------------------------------------------------------------------ catálogo

    private record Prod(String codigo, String nombre, String categoria, String proveedor, String unidad,
                        String precio, String seguridad, boolean fraccionable, double demandaDiaria,
                        boolean dejarEnAlerta) {
    }

    /**
     * Demanda pensada para que el ABC tenga pocos productos de alto valor (A) y muchos de bajo valor (C), y
     * para que algunos productos terminen en alerta de reposición.
     */
    private static final List<Prod> PRODUCTOS = List.of(
            new Prod("CER-001", "Cerveza rubia 1 L (caja x12)", "Cervezas", "Cervecería del Sur", "caja", "18500", "8", false, 6, false),
            new Prod("CER-002", "Cerveza negra 473 ml (pack x6)", "Cervezas", "Cervecería del Sur", "pack", "9800", "6", false, 3, true),
            new Prod("GAS-001", "Gaseosa cola 2,25 L (pack x6)", "Gaseosas", "Bebidas Andinas", "pack", "11200", "10", false, 9, false),
            new Prod("GAS-002", "Gaseosa lima-limón 2,25 L (pack x6)", "Gaseosas", "Bebidas Andinas", "pack", "10400", "6", false, 4, false),
            new Prod("GAS-003", "Agua tónica 1,5 L (pack x6)", "Gaseosas", "Bebidas Andinas", "pack", "8900", "4", false, 1.2, true),
            new Prod("AGU-001", "Agua mineral 2 L (pack x6)", "Aguas", "Manantial Patagónico", "pack", "5400", "12", false, 8, false),
            new Prod("AGU-002", "Agua saborizada 1,5 L (pack x6)", "Aguas", "Manantial Patagónico", "pack", "6900", "5", false, 2.5, false),
            new Prod("AGU-003", "Agua en bidón 20 L", "Aguas", "Manantial Patagónico", "un", "4200", "4", false, 1.5, false),
            new Prod("JUG-001", "Jugo de naranja 1 L (caja x8)", "Jugos", "Frutas del Valle", "caja", "12800", "3", false, 1.8, false),
            new Prod("JUG-002", "Jugo en polvo (caja x20 sobres)", "Jugos", "Frutas del Valle", "caja", "3600", "4", false, 0.8, false),
            new Prod("HIE-001", "Hielo en cubos", "Hielo", "Frío Express", "kg", "900", "20", true, 14.5, true),
            new Prod("SNK-001", "Papas fritas 150 g (caja x12)", "Snacks", "Snacks Rioplatenses", "caja", "15600", "3", false, 1.3, false),
            new Prod("SNK-002", "Maní salado 200 g (caja x24)", "Snacks", "Snacks Rioplatenses", "caja", "13900", "2", false, 0.4, false),
            new Prod("VAS-001", "Vasos descartables 300 cc (x100)", "Descartables", "Plásticos Oeste", "paquete", "2900", "5", false, 0.6, false),
            new Prod("VAS-002", "Servilletas (paquete x500)", "Descartables", "Plásticos Oeste", "paquete", "3100", "2", false, 0, false));

    /** Proveedor → lead time en días (RN-20). */
    private static final Map<String, Integer> PROVEEDORES = Map.of(
            "Cervecería del Sur", 7,
            "Bebidas Andinas", 5,
            "Manantial Patagónico", 3,
            "Frutas del Valle", 10,
            "Frío Express", 1,
            "Snacks Rioplatenses", 7,
            "Plásticos Oeste", 15);

    // ------------------------------------------------------------------ simulación

    private void generar() {
        LocalDateTime ahora = Fechas.ahoraUtc(clock);
        Long central = deposito("Central", "Depósito principal");
        Long norte = deposito("Sucursal Norte", "Punto de venta zona norte");
        Long sur = deposito("Sucursal Sur", "Punto de venta zona sur");
        List<Long> sucursales = List.of(norte, sur);
        Long operario1 = usuario("lgomez", "Lucía", "Gómez");
        Long operario2 = usuario("mperez", "Martín", "Pérez");
        List<Long> operarios = List.of(operario1, operario2);

        List<Object[]> filas = new ArrayList<>();
        int productos = 0;
        for (Prod p : PRODUCTOS) {
            Long categoriaId = categoria(p.categoria());
            Long proveedorId = proveedor(p.proveedor(), PROVEEDORES.get(p.proveedor()));
            Long productoId = producto(p, categoriaId, proveedorId);
            simular(p, productoId, central, sucursales, operarios, ahora, filas);
            productos++;
        }

        jdbc.batchUpdate("""
                INSERT INTO movimiento (tipo, producto_id, cantidad, fecha, motivo, usuario_id,
                                        deposito_origen_id, deposito_destino_id, estado_transferencia,
                                        fecha_resolucion, usuario_resolucion_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", filas);
        log.info("Perfil demo: se cargaron {} productos y {} movimientos de los últimos {} días.",
                productos, filas.size(), DIAS_DE_HISTORIA);
    }

    /** Simula día por día las compras, las ventas y las transferencias de un producto. */
    private void simular(Prod p, Long productoId, Long central, List<Long> sucursales, List<Long> operarios,
                         LocalDateTime ahora, List<Object[]> filas) {
        Map<Long, BigDecimal> stock = new HashMap<>();
        stock.put(central, BigDecimal.ZERO);
        sucursales.forEach(s -> stock.put(s, BigDecimal.ZERO));
        double demanda = p.demandaDiaria();

        for (int dia = DIAS_DE_HISTORIA; dia >= 1; dia--) {
            LocalDateTime base = ahora.minusDays(dia);

            // Compra inicial y reposiciones al depósito central
            boolean primerDia = dia == DIAS_DE_HISTORIA;
            boolean centralBajo = stock.get(central).doubleValue() < Math.max(10 * demanda, 4);
            boolean sinReponer = p.dejarEnAlerta() && dia <= 60; // se deja de comprar para que quede en alerta
            if (primerDia || (centralBajo && !sinReponer && demanda > 0)) {
                BigDecimal compra = cantidad(p, primerDia ? Math.max(45 * demanda, 20) : 30 * demanda);
                entrada(filas, productoId, central, compra, hora(base, 9), "Compra a " + p.proveedor(), operarios);
                stock.merge(central, compra, BigDecimal::add);
            }

            // Una vez por semana, abastecer las sucursales desde el central
            if (dia % 7 == 2 && demanda > 0) {
                for (Long sucursal : sucursales) {
                    BigDecimal envio = cantidad(p, 7 * demanda * 0.4);
                    if (envio.signum() > 0 && stock.get(sucursal).doubleValue() < 5 * demanda
                            && stock.get(central).compareTo(envio) >= 0) {
                        transferir(filas, p, productoId, central, sucursal, envio, base, dia, stock, operarios);
                    }
                }
            }

            // Ventas del día: 60 % sale del central y el resto de las sucursales
            if (demanda > 0) {
                vender(filas, p, productoId, central, demanda * 0.6, hora(base, 11), stock, operarios);
                for (Long sucursal : sucursales) {
                    vender(filas, p, productoId, sucursal, demanda * 0.2, hora(base, 16), stock, operarios);
                }
                // De vez en cuando, una rotura
                if (azar.nextInt(30) == 0) {
                    vender(filas, p, productoId, central, Math.max(1, demanda * 0.1), hora(base, 18), stock,
                            operarios, "Rotura");
                }
            }
        }
    }

    private void transferir(List<Object[]> filas, Prod p, Long productoId, Long origen, Long destino,
                            BigDecimal envio, LocalDateTime base, int dia, Map<Long, BigDecimal> stock,
                            List<Long> operarios) {
        LocalDateTime fecha = hora(base, 8);
        String estado;
        LocalDateTime resolucion;
        if (dia <= 2) {
            estado = "PENDIENTE"; // las del último envío quedan en tránsito
            resolucion = null;
        } else if (azar.nextInt(8) == 0) {
            estado = "CANCELADA";
            resolucion = fecha.plusHours(3);
        } else {
            estado = "CONFIRMADA";
            resolucion = fecha.plusHours(5);
        }
        filas.add(new Object[] {"TRANSFERENCIA", productoId, envio, ts(fecha), null, operario(operarios), origen,
                destino, estado, resolucion == null ? null : ts(resolucion),
                resolucion == null ? null : operario(operarios)});
        // RN-10 a RN-12: sale del origen al crearse; llega al destino solo si se confirma
        if (!"CANCELADA".equals(estado)) {
            stock.merge(origen, envio.negate(), BigDecimal::add);
        }
        if ("CONFIRMADA".equals(estado)) {
            stock.merge(destino, envio, BigDecimal::add);
        }
    }

    private void vender(List<Object[]> filas, Prod p, Long productoId, Long deposito, double promedio,
                        LocalDateTime fecha, Map<Long, BigDecimal> stock, List<Long> operarios) {
        vender(filas, p, productoId, deposito, promedio, fecha, stock, operarios, "Venta");
    }

    private void vender(List<Object[]> filas, Prod p, Long productoId, Long deposito, double promedio,
                        LocalDateTime fecha, Map<Long, BigDecimal> stock, List<Long> operarios, String motivo) {
        // Variación diaria entre 40 % y 160 % del promedio
        BigDecimal pedido = cantidadAlAzar(p, promedio * (0.4 + azar.nextDouble() * 1.2));
        BigDecimal vendido = pedido.min(stock.get(deposito)); // RN-06: nunca deja stock negativo
        if (vendido.signum() <= 0) {
            return;
        }
        filas.add(new Object[] {"SALIDA", productoId, vendido, ts(fecha), motivo, operario(operarios), deposito,
                null, null, null, null});
        stock.merge(deposito, vendido.negate(), BigDecimal::add);
    }

    private void entrada(List<Object[]> filas, Long productoId, Long deposito, BigDecimal cantidad,
                         LocalDateTime fecha, String motivo, List<Long> operarios) {
        filas.add(new Object[] {"ENTRADA", productoId, cantidad, ts(fecha), motivo, operario(operarios), null,
                deposito, null, null, null});
    }

    /** RN-44: entera si el producto no admite fracciones; si no, con dos decimales. */
    private static BigDecimal cantidad(Prod p, double valor) {
        BigDecimal v = BigDecimal.valueOf(valor);
        return p.fraccionable() ? v.setScale(2, RoundingMode.HALF_UP) : v.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Como {@link #cantidad}, pero en los productos no fraccionables redondea al azar según la parte decimal:
     * así un producto que vende 0,4 unidades por día vende 1 unidad cerca de 4 de cada 10 días.
     */
    private BigDecimal cantidadAlAzar(Prod p, double valor) {
        if (p.fraccionable()) {
            return cantidad(p, valor);
        }
        double entero = Math.floor(valor);
        return BigDecimal.valueOf((long) entero + (azar.nextDouble() < valor - entero ? 1 : 0));
    }

    /** Hora local de Argentina con algunos minutos al azar (Argentina es UTC-3). */
    private LocalDateTime hora(LocalDateTime base, int horaArgentina) {
        return base.toLocalDate().atTime(horaArgentina + 3, azar.nextInt(60));
    }

    private Long operario(List<Long> operarios) {
        return operarios.get(azar.nextInt(operarios.size()));
    }

    /** Se pasa como LocalDateTime para que el driver no la convierta a la zona horaria de la JVM. */
    private static LocalDateTime ts(LocalDateTime fecha) {
        return fecha;
    }

    // ------------------------------------------------------------------ altas (reutiliza lo que ya exista)

    private Long deposito(String nombre, String ubicacion) {
        return buscarOInsertar("SELECT id FROM deposito WHERE nombre = ?", nombre,
                "INSERT INTO deposito (nombre, ubicacion, activo) VALUES (?, ?, TRUE)", nombre, ubicacion);
    }

    private Long categoria(String nombre) {
        return buscarOInsertar("SELECT id FROM categoria WHERE nombre = ?", nombre,
                "INSERT INTO categoria (nombre, activo) VALUES (?, TRUE)", nombre);
    }

    private Long proveedor(String nombre, int leadTime) {
        return buscarOInsertar("SELECT id FROM proveedor WHERE nombre = ?", nombre,
                "INSERT INTO proveedor (nombre, lead_time_dias, activo) VALUES (?, ?, TRUE)", nombre, leadTime);
    }

    private Long producto(Prod p, Long categoriaId, Long proveedorId) {
        return buscarOInsertar("SELECT id FROM producto WHERE codigo = ?", p.codigo(), """
                INSERT INTO producto (codigo, nombre, categoria_id, proveedor_id, unidad_medida, precio_referencia,
                                      stock_seguridad, fraccionable, activo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)""",
                p.codigo(), p.nombre(), categoriaId, proveedorId, p.unidad(), new BigDecimal(p.precio()),
                new BigDecimal(p.seguridad()), p.fraccionable());
    }

    private Long usuario(String username, String nombre, String apellido) {
        return buscarOInsertar("SELECT id FROM usuario WHERE username = ?", username, """
                INSERT INTO usuario (username, password_hash, nombre, apellido, rol, activo)
                VALUES (?, ?, ?, ?, 'OPERARIO', TRUE)""",
                username, passwordEncoder.encode(CLAVE_OPERARIOS), nombre, apellido);
    }

    private Long buscarOInsertar(String select, Object clave, String insert, Object... valores) {
        List<Long> existentes = jdbc.queryForList(select, Long.class, clave);
        if (!existentes.isEmpty()) {
            return existentes.get(0);
        }
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < valores.length; i++) {
                ps.setObject(i + 1, valores[i]);
            }
            return ps;
        }, key);
        return key.getKey().longValue();
    }
}
