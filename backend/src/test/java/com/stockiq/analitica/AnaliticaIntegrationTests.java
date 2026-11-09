package com.stockiq.analitica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockiq.AbstractIntegrationTest;
import com.stockiq.analitica.repository.AnaliticaConsultaRepository;
import com.stockiq.catalogo.model.Categoria;
import com.stockiq.catalogo.model.Producto;
import com.stockiq.catalogo.model.Proveedor;
import com.stockiq.catalogo.repository.CategoriaRepository;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.catalogo.repository.ProveedorRepository;
import com.stockiq.deposito.model.Deposito;
import com.stockiq.deposito.repository.DepositoRepository;
import com.stockiq.usuario.model.Rol;
import com.stockiq.usuario.model.Usuario;
import com.stockiq.usuario.repository.UsuarioRepository;

/**
 * Consultas de la capa analítica contra un MySQL real (RN-18, RN-19, RN-36) y permisos de los endpoints
 * (RN-35). Los movimientos se insertan con fechas en el pasado, como el script de datos de prueba (RN-41).
 * La base es compartida con otros tests: cada caso usa sus propios productos y solo mira sus resultados.
 */
@AutoConfigureMockMvc
class AnaliticaIntegrationTests extends AbstractIntegrationTest {

    private static final String CLAVE = "clave-segura-123";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 6, 1, 12, 0);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private AnaliticaConsultaRepository consultas;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private DepositoRepository depositoRepository;

    private Usuario encargado;
    private Producto producto;
    private Deposito origen;
    private Deposito destino;

    @BeforeEach
    void setUp() {
        encargado = crearUsuario(Rol.ENCARGADO);
        producto = crearProducto();
        origen = crearDeposito(true);
        destino = crearDeposito(true);
    }

    // ------------------------------------------------------------------ RN-36: stock a una fecha

    @Test
    void stockTotalAl_consideraElEstadoDeLasTransferenciasEnEseMomento_RN36() {
        insertar("ENTRADA", "100", T0, null, destino, null, null);
        // Transferencia creada en T0+1h y confirmada en T0+5h: en tránsito entre esas horas
        insertar("TRANSFERENCIA", "30", T0.plusHours(1), origen, destino, "CONFIRMADA", T0.plusHours(5));
        // Transferencia creada en T0+2h y cancelada en T0+3h: en tránsito solo en ese rato
        insertar("TRANSFERENCIA", "10", T0.plusHours(2), origen, destino, "CANCELADA", T0.plusHours(3));
        insertar("SALIDA", "20", T0.plusDays(1), destino, null, null, null);

        assertThat(stockAl(T0)).isEqualByComparingTo("0");                  // antes de todo
        assertThat(stockAl(T0.plusMinutes(30))).isEqualByComparingTo("100"); // solo la entrada
        assertThat(stockAl(T0.plusMinutes(150))).isEqualByComparingTo("60"); // 100 − 30 − 10 en tránsito
        assertThat(stockAl(T0.plusHours(4))).isEqualByComparingTo("70");     // la cancelada ya volvió
        assertThat(stockAl(T0.plusHours(6))).isEqualByComparingTo("100");    // la confirmada ya llegó
        assertThat(stockAl(T0.plusDays(2))).isEqualByComparingTo("80");      // después de la salida
    }

    @Test
    void salidasPorProducto_soloCuentaSalidasDentroDeLaVentana_RN19() {
        insertar("ENTRADA", "100", T0, null, origen, null, null);
        insertar("SALIDA", "5", T0.plusDays(1), origen, null, null, null);
        insertar("SALIDA", "7", T0.plusDays(10), origen, null, null, null);
        insertar("TRANSFERENCIA", "20", T0.plusDays(2), origen, destino, "CONFIRMADA", T0.plusDays(2).plusHours(1));

        Map<Long, BigDecimal> salidas = consultas.salidasPorProducto(T0, T0.plusDays(5));

        assertThat(salidas.get(producto.getId())).isEqualByComparingTo("5");
    }

    @Test
    void stockDisponible_noSumaLosDepositosInactivos_RN18() {
        Deposito inactivo = crearDeposito(true);
        insertar("ENTRADA", "40", T0, null, origen, null, null);
        insertar("ENTRADA", "15", T0, null, inactivo, null, null);
        inactivo.setActivo(false);
        depositoRepository.save(inactivo);

        assertThat(consultas.stockDisponiblePorProducto().get(producto.getId())).isEqualByComparingTo("40");
    }

    // ------------------------------------------------------------------ endpoints y permisos

    @Test
    void endpointsAnaliticos_respondenAlEncargado() throws Exception {
        String token = login(encargado);
        for (String ruta : new String[] {"/analitica/reposicion", "/analitica/rotacion/productos",
                "/analitica/rotacion/categorias", "/analitica/abc", "/dashboard/analitica", "/dashboard/stock",
                "/dashboard/evolucion?agrupacion=MES", "/parametros"}) {
            mockMvc.perform(get(ruta).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        }
        mockMvc.perform(get("/analitica/abc").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.resumen.length()").value(3));
    }

    @Test
    void elOperarioNoVeLaCapaAnalitica_peroSiElStock_RN35() throws Exception {
        String token = login(crearUsuario(Rol.OPERARIO));
        for (String ruta : new String[] {"/analitica/abc", "/analitica/reposicion", "/dashboard/analitica",
                "/dashboard/evolucion", "/parametros"}) {
            mockMvc.perform(get(ruta).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/dashboard/stock").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test
    void actualizarParametroInvalido_responde400_RN37() throws Exception {
        String token = login(encargado);
        mockMvc.perform(put("/parametros/corte_abc_a").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":\"99\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CORTES_ABC_INVALIDOS"));
    }

    // ------------------------------------------------------------------ ayudas

    private BigDecimal stockAl(LocalDateTime instante) {
        return consultas.stockTotalAl(instante).getOrDefault(producto.getId(), BigDecimal.ZERO);
    }

    private void insertar(String tipo, String cantidad, LocalDateTime fecha, Deposito desde, Deposito hacia,
                          String estado, LocalDateTime resolucion) {
        jdbc.update("""
                INSERT INTO movimiento (tipo, producto_id, cantidad, fecha, motivo, usuario_id, deposito_origen_id,
                                        deposito_destino_id, estado_transferencia, fecha_resolucion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                tipo, producto.getId(), new BigDecimal(cantidad), fecha, "SALIDA".equals(tipo) ? "Venta" : null,
                encargado.getId(), desde == null ? null : desde.getId(), hacia == null ? null : hacia.getId(),
                estado, resolucion);
    }

    private static String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Usuario crearUsuario(Rol rol) {
        return usuarioRepository.save(Usuario.builder()
                .username("u-" + sufijo()).passwordHash(passwordEncoder.encode(CLAVE))
                .nombre("Nombre").apellido("Apellido").rol(rol).activo(true).build());
    }

    private String login(Usuario usuario) throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of("username", usuario.getUsername(), "password", CLAVE));
        String respuesta = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("token").asText();
    }

    private Producto crearProducto() {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nombre("Cat " + sufijo()).build());
        Proveedor proveedor = proveedorRepository.save(Proveedor.builder().nombre("Prov " + sufijo())
                .leadTimeDias(5).build());
        return productoRepository.save(Producto.builder()
                .codigo("P-" + sufijo()).nombre("Producto " + sufijo())
                .categoria(categoria).proveedor(proveedor)
                .unidadMedida("un").precioReferencia(new BigDecimal("100.00"))
                .stockSeguridad(BigDecimal.ZERO).fraccionable(false).build());
    }

    private Deposito crearDeposito(boolean activo) {
        return depositoRepository.save(Deposito.builder().nombre("Dep " + sufijo()).activo(activo).build());
    }
}
