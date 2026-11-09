package com.stockiq.analitica.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stockiq.analitica.dto.ReposicionResponse;
import com.stockiq.analitica.repository.AnaliticaConsultaRepository;
import com.stockiq.catalogo.model.Producto;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.parametro.service.ParametroService;
import com.stockiq.parametro.service.ParametrosAnalitica;
import com.stockiq.stock.repository.StockConsultaRepository;

@ExtendWith(MockitoExtension.class)
class ReposicionServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-02T15:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private AnaliticaConsultaRepository consultas;
    @Mock
    private StockConsultaRepository stockConsulta;
    @Mock
    private ParametroService parametroService;

    private ReposicionService servicio;

    @BeforeEach
    void setUp() {
        servicio = new ReposicionService(productoRepository, consultas, stockConsulta, parametroService, RELOJ);
    }

    private static BigDecimal n(String valor) {
        return new BigDecimal(valor);
    }

    // ------------------------------------------------------------------ cálculo de un producto

    @Test
    void puntoDeReposicion_esConsumoPorLeadTimeMasStockDeSeguridad_RN20() {
        // consumo 2/día, lead time 5 días, seguridad 10 → PR = 2 × 5 + 10 = 20
        Producto p = Datos.producto(1, "100", 5, "10", false, Datos.BEBIDAS);

        ReposicionResponse r = servicio.calcular(p, n("2"), n("50"), n("0"), 30);

        assertThat(r.puntoReposicion()).isEqualByComparingTo("20");
        assertThat(r.alerta()).isFalse();
    }

    @Test
    void sinSalidasEnLaVentana_elPuntoEsElStockDeSeguridad_RN21() {
        Producto p = Datos.producto(1, "100", 7, "12", false, Datos.BEBIDAS);

        ReposicionResponse r = servicio.calcular(p, n("0"), n("30"), n("0"), 30);

        assertThat(r.consumoDiario()).isEqualByComparingTo("0");
        assertThat(r.puntoReposicion()).isEqualByComparingTo("12");
    }

    @Test
    void conLeadTimeCero_elPuntoEsElStockDeSeguridad_RN22() {
        Producto p = Datos.producto(1, "100", 0, "8", false, Datos.BEBIDAS);

        ReposicionResponse r = servicio.calcular(p, n("3"), n("30"), n("0"), 30);

        assertThat(r.puntoReposicion()).isEqualByComparingTo("8");
    }

    @Test
    void stockIgualAlPunto_generaAlerta_RN23() {
        Producto p = Datos.producto(1, "100", 5, "10", false, Datos.BEBIDAS);

        ReposicionResponse r = servicio.calcular(p, n("2"), n("20"), n("0"), 30);

        assertThat(r.alerta()).isTrue();
    }

    @Test
    void cantidadSugerida_descuentaStockYTransito_RN24() {
        // PR 20 + consumo 2 × 30 días de cobertura − stock 15 − tránsito 5 = 60
        Producto p = Datos.producto(1, "100", 5, "10", false, Datos.BEBIDAS);

        ReposicionResponse r = servicio.calcular(p, n("2"), n("15"), n("5"), 30);

        assertThat(r.cantidadSugerida()).isEqualByComparingTo("60");
    }

    @Test
    void cantidadSugerida_nuncaEsNegativa_RN24() {
        Producto p = Datos.producto(1, "100", 5, "10", false, Datos.BEBIDAS);

        ReposicionResponse r = servicio.calcular(p, n("1"), n("500"), n("0"), 30);

        assertThat(r.cantidadSugerida()).isEqualByComparingTo("0");
    }

    @Test
    void cantidadSugerida_paraProductoNoFraccionable_seRedondeaHaciaArriba_RN44() {
        // PR 1,5 + 0,3 × 10 − 0 = 4,5 → 5
        Producto p = Datos.producto(1, "100", 5, "0", false, Datos.BEBIDAS);

        ReposicionResponse r = servicio.calcular(p, n("0.3"), n("0"), n("0"), 10);

        assertThat(r.cantidadSugerida()).isEqualByComparingTo("5");
    }

    // ------------------------------------------------------------------ listado

    @Test
    void calcular_usaLaVentanaDeConsumoYOrdenaLasAlertasPrimero_RN19() {
        Producto conStock = Datos.producto(1, "100", 5, "0", false, Datos.BEBIDAS);
        Producto enAlerta = Datos.producto(2, "100", 5, "10", false, Datos.BEBIDAS);
        when(parametroService.configuracion()).thenReturn(ParametrosAnalitica.POR_DEFECTO);
        when(productoRepository.buscar(null, null, true, null)).thenReturn(List.of(conStock, enAlerta));
        // 60 unidades en 30 días → consumo 2/día
        when(consultas.salidasPorProducto(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Map.of(1L, n("60"), 2L, n("60")));
        when(consultas.stockDisponiblePorProducto()).thenReturn(Map.of(1L, n("100"), 2L, n("5")));
        when(stockConsulta.enTransitoPorProducto(null)).thenReturn(Map.of());

        List<ReposicionResponse> todos = servicio.calcular(false);

        assertThat(todos).extracting(r -> r.producto().id()).containsExactly(2L, 1L);
        assertThat(todos.get(0).consumoDiario()).isEqualByComparingTo("2");
        assertThat(todos.get(0).alerta()).isTrue();
    }

    @Test
    void calcular_soloAlertas_filtraLosProductosSinAlerta() {
        Producto conStock = Datos.producto(1, "100", 5, "0", false, Datos.BEBIDAS);
        when(parametroService.configuracion()).thenReturn(ParametrosAnalitica.POR_DEFECTO);
        when(productoRepository.buscar(null, null, true, null)).thenReturn(List.of(conStock));
        when(consultas.salidasPorProducto(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Map.of());
        when(consultas.stockDisponiblePorProducto()).thenReturn(Map.of(1L, n("10")));
        when(stockConsulta.enTransitoPorProducto(null)).thenReturn(Map.of());

        assertThat(servicio.calcular(true)).isEmpty();
    }
}
