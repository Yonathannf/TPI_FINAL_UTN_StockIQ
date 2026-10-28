package com.stockiq.movimiento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stockiq.catalogo.model.Producto;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.common.exception.BadRequestException;
import com.stockiq.common.exception.BusinessException;
import com.stockiq.common.exception.NotFoundException;
import com.stockiq.deposito.model.Deposito;
import com.stockiq.deposito.repository.DepositoRepository;
import com.stockiq.movimiento.dto.EntradaRequest;
import com.stockiq.movimiento.dto.MovimientoResponse;
import com.stockiq.movimiento.dto.SalidaRequest;
import com.stockiq.movimiento.dto.TransferenciaRequest;
import com.stockiq.movimiento.mapper.MovimientoMapper;
import com.stockiq.movimiento.model.EstadoTransferencia;
import com.stockiq.movimiento.model.Movimiento;
import com.stockiq.movimiento.model.TipoMovimiento;
import com.stockiq.movimiento.repository.MovimientoRepository;
import com.stockiq.security.UsuarioActual;
import com.stockiq.stock.repository.StockConsultaRepository;
import com.stockiq.usuario.model.Rol;
import com.stockiq.usuario.model.Usuario;
import com.stockiq.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class MovimientoServiceTest {

    private static final UsuarioActual ACTUAL = new UsuarioActual(7L, "operario", Rol.OPERARIO);

    @Mock
    private MovimientoRepository movimientoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private DepositoRepository depositoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private StockConsultaRepository stockConsulta;

    private MovimientoService service;

    @BeforeEach
    void setUp() {
        service = new MovimientoService(movimientoRepository, productoRepository, depositoRepository,
                usuarioRepository, stockConsulta, new MovimientoMapper());
    }

    private static Producto producto(boolean activo, boolean fraccionable) {
        return Producto.builder().id(1L).codigo("P-1").nombre("Agua").unidadMedida("un")
                .activo(activo).fraccionable(fraccionable).build();
    }

    private static Deposito deposito(Long id, boolean activo) {
        return Deposito.builder().id(id).nombre("Depósito " + id).activo(activo).build();
    }

    private static Usuario usuario() {
        return Usuario.builder().id(7L).username("operario").nombre("Ana").apellido("Pérez")
                .rol(Rol.OPERARIO).activo(true).build();
    }

    private void productoExiste(Producto p) {
        when(productoRepository.bloquearPorId(p.getId())).thenReturn(Optional.of(p));
    }

    private void depositoExiste(Deposito d) {
        when(depositoRepository.bloquearPorId(d.getId())).thenReturn(Optional.of(d));
    }

    private void guardarDevuelveElMismo() {
        when(usuarioRepository.getReferenceById(7L)).thenReturn(usuario());
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(inv -> {
            Movimiento m = inv.getArgument(0);
            m.setId(99L);
            return m;
        });
    }

    private static Movimiento transferencia(EstadoTransferencia estado) {
        return Movimiento.builder().id(5L).tipo(TipoMovimiento.TRANSFERENCIA)
                .producto(producto(true, true)).cantidad(new BigDecimal("4.00"))
                .fecha(LocalDateTime.now(ZoneOffset.UTC)).usuario(usuario())
                .depositoOrigen(deposito(1L, true)).depositoDestino(deposito(2L, true))
                .estadoTransferencia(estado).build();
    }

    private static void assertCodigo(Throwable error, String codigo) {
        assertThat(error).isInstanceOf(BusinessException.class);
        assertThat(((BusinessException) error).getCodigo()).isEqualTo(codigo);
    }

    // ------------------------------------------------------------ entradas

    @Test
    void entrada_guardaConFechaYUsuarioDelServidor() {
        productoExiste(producto(true, true));
        depositoExiste(deposito(2L, true));
        guardarDevuelveElMismo();

        MovimientoResponse r = service.registrarEntrada(
                new EntradaRequest(1L, 2L, new BigDecimal("10.50"), "  compra  "), ACTUAL);

        assertThat(r.tipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(r.cantidad()).isEqualByComparingTo("10.50");
        assertThat(r.motivo()).isEqualTo("compra");
        assertThat(r.depositoOrigen()).isNull();
        assertThat(r.depositoDestino().id()).isEqualTo(2L);
        assertThat(r.usuario().id()).isEqualTo(7L);
        assertThat(r.estadoTransferencia()).isNull();
        assertThat(r.fecha().getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void entrada_enProductoInactivo_esConflicto() {
        productoExiste(producto(false, true));

        assertThatThrownBy(() -> service.registrarEntrada(
                new EntradaRequest(1L, 2L, BigDecimal.ONE, null), ACTUAL))
                .satisfies(e -> assertCodigo(e, "PRODUCTO_INACTIVO"));
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void entrada_enDepositoInactivo_esConflicto() {
        productoExiste(producto(true, true));
        depositoExiste(deposito(2L, false));

        assertThatThrownBy(() -> service.registrarEntrada(
                new EntradaRequest(1L, 2L, BigDecimal.ONE, null), ACTUAL))
                .satisfies(e -> assertCodigo(e, "DEPOSITO_INACTIVO"));
    }

    @Test
    void entrada_productoInexistente_esNoEncontrado() {
        when(productoRepository.bloquearPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarEntrada(
                new EntradaRequest(1L, 2L, BigDecimal.ONE, null), ACTUAL))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void cantidadConDecimales_enProductoNoFraccionable_esRechazada() {
        productoExiste(producto(true, false));

        assertThatThrownBy(() -> service.registrarEntrada(
                new EntradaRequest(1L, 2L, new BigDecimal("2.50"), null), ACTUAL))
                .satisfies(e -> assertCodigo(e, "CANTIDAD_NO_ENTERA"));
    }

    @Test
    void cantidadEnteraConCeros_enProductoNoFraccionable_seAcepta() {
        productoExiste(producto(true, false));
        depositoExiste(deposito(2L, true));
        guardarDevuelveElMismo();

        MovimientoResponse r = service.registrarEntrada(
                new EntradaRequest(1L, 2L, new BigDecimal("10.00"), null), ACTUAL);

        assertThat(r.cantidad()).isEqualByComparingTo("10");
    }

    // -------------------------------------------------------------- salidas

    @Test
    void salida_sinMotivo_esRechazadaAntesDeTocarLaBase() {
        assertThatThrownBy(() -> service.registrarSalida(
                new SalidaRequest(1L, 1L, BigDecimal.ONE, "   "), ACTUAL))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> assertCodigo(e, "MOTIVO_OBLIGATORIO"));
        verify(productoRepository, never()).bloquearPorId(any());
    }

    @Test
    void salida_conStockInsuficiente_esConflicto() {
        productoExiste(producto(true, true));
        depositoExiste(deposito(1L, true));
        when(stockConsulta.stockEn(1L, 1L)).thenReturn(new BigDecimal("5.00"));

        assertThatThrownBy(() -> service.registrarSalida(
                new SalidaRequest(1L, 1L, new BigDecimal("5.01"), "venta"), ACTUAL))
                .satisfies(e -> assertCodigo(e, "STOCK_INSUFICIENTE"));
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void salida_conStockJustoAlcanza() {
        productoExiste(producto(true, true));
        depositoExiste(deposito(1L, true));
        when(stockConsulta.stockEn(1L, 1L)).thenReturn(new BigDecimal("5.00"));
        guardarDevuelveElMismo();

        MovimientoResponse r = service.registrarSalida(
                new SalidaRequest(1L, 1L, new BigDecimal("5.00"), "venta"), ACTUAL);

        assertThat(r.tipo()).isEqualTo(TipoMovimiento.SALIDA);
        assertThat(r.depositoOrigen().id()).isEqualTo(1L);
        assertThat(r.depositoDestino()).isNull();
        assertThat(r.motivo()).isEqualTo("venta");
    }

    // ------------------------------------------------------- transferencias

    @Test
    void transferencia_mismoDeposito_esRechazada() {
        assertThatThrownBy(() -> service.registrarTransferencia(
                new TransferenciaRequest(1L, 3L, 3L, BigDecimal.ONE, null), ACTUAL))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> assertCodigo(e, "DEPOSITOS_IGUALES"));
    }

    @Test
    void transferencia_quedaPendiente() {
        productoExiste(producto(true, true));
        depositoExiste(deposito(1L, true));
        depositoExiste(deposito(2L, true));
        when(stockConsulta.stockEn(1L, 1L)).thenReturn(new BigDecimal("10.00"));
        guardarDevuelveElMismo();

        MovimientoResponse r = service.registrarTransferencia(
                new TransferenciaRequest(1L, 1L, 2L, new BigDecimal("4.00"), null), ACTUAL);

        assertThat(r.tipo()).isEqualTo(TipoMovimiento.TRANSFERENCIA);
        assertThat(r.estadoTransferencia()).isEqualTo(EstadoTransferencia.PENDIENTE);
        assertThat(r.fechaResolucion()).isNull();
        assertThat(r.usuarioResolucion()).isNull();
    }

    @Test
    void transferencia_conStockInsuficienteEnElOrigen_esConflicto() {
        productoExiste(producto(true, true));
        depositoExiste(deposito(1L, true));
        depositoExiste(deposito(2L, true));
        when(stockConsulta.stockEn(1L, 1L)).thenReturn(new BigDecimal("3.00"));

        assertThatThrownBy(() -> service.registrarTransferencia(
                new TransferenciaRequest(1L, 1L, 2L, new BigDecimal("4.00"), null), ACTUAL))
                .satisfies(e -> assertCodigo(e, "STOCK_INSUFICIENTE"));
    }

    // ------------------------------------------------- confirmar y cancelar

    @Test
    void confirmar_registraEstadoFechaYUsuarioDeResolucion() {
        Movimiento m = transferencia(EstadoTransferencia.PENDIENTE);
        when(movimientoRepository.bloquearPorId(5L)).thenReturn(Optional.of(m));
        when(usuarioRepository.getReferenceById(7L)).thenReturn(usuario());

        MovimientoResponse r = service.confirmar(5L, ACTUAL);

        assertThat(r.estadoTransferencia()).isEqualTo(EstadoTransferencia.CONFIRMADA);
        assertThat(r.fechaResolucion()).isNotNull();
        assertThat(r.usuarioResolucion().id()).isEqualTo(7L);
    }

    @Test
    void cancelar_marcaCancelada() {
        Movimiento m = transferencia(EstadoTransferencia.PENDIENTE);
        when(movimientoRepository.bloquearPorId(5L)).thenReturn(Optional.of(m));
        when(usuarioRepository.getReferenceById(7L)).thenReturn(usuario());

        assertThat(service.cancelar(5L, ACTUAL).estadoTransferencia()).isEqualTo(EstadoTransferencia.CANCELADA);
    }

    @Test
    void transferenciaYaResuelta_noCambiaDeEstado() {
        Movimiento m = transferencia(EstadoTransferencia.CONFIRMADA);
        when(movimientoRepository.bloquearPorId(5L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.cancelar(5L, ACTUAL))
                .satisfies(e -> assertCodigo(e, "TRANSFERENCIA_NO_PENDIENTE"));
        assertThat(m.getEstadoTransferencia()).isEqualTo(EstadoTransferencia.CONFIRMADA);
    }

    @Test
    void confirmar_unMovimientoQueNoEsTransferencia_esConflicto() {
        Movimiento m = transferencia(null);
        m.setTipo(TipoMovimiento.ENTRADA);
        when(movimientoRepository.bloquearPorId(5L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.confirmar(5L, ACTUAL))
                .satisfies(e -> assertCodigo(e, "TRANSFERENCIA_NO_PENDIENTE"));
    }

    @Test
    void confirmar_inexistente_esNoEncontrado() {
        when(movimientoRepository.bloquearPorId(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmar(5L, ACTUAL)).isInstanceOf(NotFoundException.class);
    }

    // ------------------------------------------------------------ historial

    @Test
    void historial_conDesdePosteriorAHasta_esRechazado() {
        assertThatThrownBy(() -> service.historial(null, null, null, null,
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 10), 0, 20))
                .satisfies(e -> assertCodigo(e, "RANGO_FECHAS_INVALIDO"));
    }

    @Test
    void diaDeArgentina_seConvierteAUtcSumandoTresHoras() {
        assertThat(MovimientoService.inicioDelDiaEnUtc(LocalDate.of(2026, 10, 22)))
                .isEqualTo(LocalDateTime.of(2026, 10, 22, 3, 0));
    }
}
