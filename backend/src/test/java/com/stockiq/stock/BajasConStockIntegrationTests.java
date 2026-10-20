package com.stockiq.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stockiq.AbstractIntegrationTest;
import com.stockiq.catalogo.dto.ProductoRequest;
import com.stockiq.catalogo.model.Categoria;
import com.stockiq.catalogo.model.Producto;
import com.stockiq.catalogo.model.Proveedor;
import com.stockiq.catalogo.repository.CategoriaRepository;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.catalogo.repository.ProveedorRepository;
import com.stockiq.catalogo.service.ProductoService;
import com.stockiq.common.exception.ConflictException;
import com.stockiq.deposito.model.Deposito;
import com.stockiq.deposito.repository.DepositoRepository;
import com.stockiq.deposito.service.DepositoService;
import com.stockiq.usuario.model.Rol;
import com.stockiq.usuario.model.Usuario;
import com.stockiq.usuario.repository.UsuarioRepository;

/**
 * Bajas de depósitos y productos (RN-38, RN-39) y paso a no fraccionable (RN-47) contra un MySQL real.
 * Verifica las consultas nativas de StockConsultaRepository sobre las vistas v_stock_deposito y
 * v_stock_transito. Como el módulo de movimientos todavía no existe, los movimientos se insertan
 * directamente en la tabla.
 */
class BajasConStockIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;
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
    @Autowired
    private DepositoService depositoService;
    @Autowired
    private ProductoService productoService;

    private Long usuarioId;

    @BeforeEach
    void setUp() {
        usuarioId = usuarioRepository.save(Usuario.builder()
                .username("u-" + sufijo())
                .passwordHash("hash")
                .nombre("Nombre").apellido("Apellido")
                .rol(Rol.OPERARIO).activo(true).build()).getId();
    }

    private static String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Deposito deposito() {
        return depositoRepository.save(Deposito.builder().nombre("Dep " + sufijo()).activo(true).build());
    }

    private Producto producto(boolean fraccionable) {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nombre("Cat " + sufijo()).build());
        Proveedor proveedor = proveedorRepository.save(Proveedor.builder().nombre("Prov " + sufijo())
                .leadTimeDias(5).build());
        return productoRepository.save(Producto.builder()
                .codigo("P-" + sufijo()).nombre("Producto")
                .categoria(categoria).proveedor(proveedor)
                .unidadMedida("un").precioReferencia(new BigDecimal("100.00"))
                .stockSeguridad(BigDecimal.ZERO).fraccionable(fraccionable)
                .build());
    }

    private void entrada(Producto producto, Deposito destino, String cantidad) {
        jdbc.update("""
                INSERT INTO movimiento (tipo, producto_id, cantidad, fecha, usuario_id, deposito_destino_id)
                VALUES ('ENTRADA', ?, ?, UTC_TIMESTAMP(), ?, ?)""",
                producto.getId(), new BigDecimal(cantidad), usuarioId, destino.getId());
    }

    private void salida(Producto producto, Deposito origen, String cantidad) {
        jdbc.update("""
                INSERT INTO movimiento (tipo, producto_id, cantidad, fecha, motivo, usuario_id, deposito_origen_id)
                VALUES ('SALIDA', ?, ?, UTC_TIMESTAMP(), 'venta', ?, ?)""",
                producto.getId(), new BigDecimal(cantidad), usuarioId, origen.getId());
    }

    private void transferenciaPendiente(Producto producto, Deposito origen, Deposito destino, String cantidad) {
        jdbc.update("""
                INSERT INTO movimiento (tipo, producto_id, cantidad, fecha, usuario_id,
                                        deposito_origen_id, deposito_destino_id, estado_transferencia)
                VALUES ('TRANSFERENCIA', ?, ?, UTC_TIMESTAMP(), ?, ?, ?, 'PENDIENTE')""",
                producto.getId(), new BigDecimal(cantidad), usuarioId, origen.getId(), destino.getId());
    }

    private static ProductoRequest comoRequest(Producto p, boolean fraccionable) {
        return new ProductoRequest(p.getCodigo(), p.getNombre(), p.getCategoria().getId(),
                p.getProveedor().getId(), p.getUnidadMedida(), p.getPrecioReferencia(),
                p.getStockSeguridad(), fraccionable);
    }

    // --------------------------------------------------------------- depósitos (RN-38)

    @Test
    void desactivarDeposito_conStock_responde409_RN38() {
        Deposito deposito = deposito();
        entrada(producto(true), deposito, "10");

        assertThatThrownBy(() -> depositoService.desactivar(deposito.getId()))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("DEPOSITO_CON_STOCK");
    }

    @Test
    void desactivarDeposito_vaciadoConUnaSalida_loDesactiva() {
        Deposito deposito = deposito();
        Producto producto = producto(true);
        entrada(producto, deposito, "10");
        salida(producto, deposito, "10");

        assertThat(depositoService.desactivar(deposito.getId()).activo()).isFalse();
    }

    @Test
    void desactivarDeposito_destinoDeUnaTransferenciaPendiente_responde409_RN38() {
        Deposito origen = deposito();
        Deposito destino = deposito();
        Producto producto = producto(true);
        entrada(producto, origen, "5");
        transferenciaPendiente(producto, origen, destino, "5");

        // El destino todavía no recibió nada (stock 0), pero la transferencia está pendiente
        assertThatThrownBy(() -> depositoService.desactivar(destino.getId()))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("DEPOSITO_CON_TRANSFERENCIAS");
    }

    // --------------------------------------------------------------- productos (RN-39, RN-47)

    @Test
    void desactivarProducto_conTodoElStockEnTransito_responde409_RN39() {
        Deposito origen = deposito();
        Deposito destino = deposito();
        Producto producto = producto(true);
        entrada(producto, origen, "5");
        transferenciaPendiente(producto, origen, destino, "5");

        // Stock 0 en ambos depósitos: las 5 unidades están en tránsito
        assertThatThrownBy(() -> productoService.desactivar(producto.getId()))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("PRODUCTO_CON_TRANSFERENCIAS");
    }

    @Test
    void pasarANoFraccionable_conStockDecimal_responde409_RN47() {
        Producto producto = producto(true);
        entrada(producto, deposito(), "2.5");

        assertThatThrownBy(() -> productoService.editar(producto.getId(), comoRequest(producto, false)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("STOCK_NO_ENTERO");
    }

    @Test
    void pasarANoFraccionable_conStockEntero_loPermite() {
        Producto producto = producto(true);
        entrada(producto, deposito(), "3");

        assertThat(productoService.editar(producto.getId(), comoRequest(producto, false)).fraccionable())
                .isFalse();
    }
}
