package com.stockiq.analitica.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.stockiq.analitica.dto.AbcProductoResponse;
import com.stockiq.analitica.dto.AbcResponse;
import com.stockiq.analitica.dto.ClaseAbc;
import com.stockiq.catalogo.model.Producto;
import com.stockiq.parametro.service.ParametrosAnalitica;

/** RN-27, RN-28 y D-12 sobre la clasificación pura (sin base de datos). */
class AbcServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-02T15:00:00Z"), ZoneOffset.UTC);
    private static final Periodo PERIODO = Periodo.de(null, null, 90, RELOJ);
    private static final ParametrosAnalitica P = ParametrosAnalitica.POR_DEFECTO;

    private final AbcService servicio = new AbcService(null, null, null, RELOJ);

    private AbcResponse clasificar(List<Producto> productos, Map<Long, BigDecimal> salidas) {
        return servicio.clasificar(productos, salidas, P, PERIODO);
    }

    private static List<ClaseAbc> clases(AbcResponse r) {
        return r.productos().stream().map(AbcProductoResponse::clase).toList();
    }

    @Test
    void catalogoVacio_devuelveResultadoVacio_RN28() {
        AbcResponse r = clasificar(List.of(), Map.of());

        assertThat(r.productos()).isEmpty();
        assertThat(r.valorTotal()).isEqualByComparingTo("0");
    }

    @Test
    void unSoloProductoConConsumo_esClaseA_RN28() {
        AbcResponse r = clasificar(List.of(Datos.producto(1, "10")), Map.of(1L, new BigDecimal("5")));

        assertThat(clases(r)).containsExactly(ClaseAbc.A);
    }

    @Test
    void elProductoQueCruzaElCorte_quedaEnLaClaseSuperior_D12() {
        // Valores 70, 20, 6 y 4 (% del total): acumulados previos 0, 70, 90 y 96 → A, A, B, C
        AbcResponse r = clasificar(
                List.of(Datos.producto(1, "1"), Datos.producto(2, "1"), Datos.producto(3, "1"), Datos.producto(4, "1")),
                Map.of(1L, new BigDecimal("70"), 2L, new BigDecimal("20"), 3L, new BigDecimal("6"),
                        4L, new BigDecimal("4")));

        assertThat(clases(r)).containsExactly(ClaseAbc.A, ClaseAbc.A, ClaseAbc.B, ClaseAbc.C);
        assertThat(r.productos().get(1).porcentajeAcumulado()).isEqualByComparingTo("90");
    }

    @Test
    void unProductoConMasDel80Porciento_esA_yElSiguienteB() {
        AbcResponse r = clasificar(List.of(Datos.producto(1, "1"), Datos.producto(2, "1"), Datos.producto(3, "1")),
                Map.of(1L, new BigDecimal("85"), 2L, new BigDecimal("15")));

        assertThat(clases(r)).containsExactly(ClaseAbc.A, ClaseAbc.B, ClaseAbc.C);
    }

    @Test
    void productosSinConsumo_sonClaseC_yNingunConsumoDejaTodoEnC_RN28() {
        AbcResponse r = clasificar(List.of(Datos.producto(1, "1"), Datos.producto(2, "1")), Map.of());

        assertThat(clases(r)).containsOnly(ClaseAbc.C);
    }

    @Test
    void elValorEsSalidasPorPrecio_yLosEmpatesSeOrdenanPorId_RN27() {
        // Producto 2: 10 × 5 = 50; producto 1: 5 × 10 = 50 → empate, primero el id 1
        AbcResponse r = clasificar(List.of(Datos.producto(2, "5"), Datos.producto(1, "10")),
                Map.of(1L, new BigDecimal("5"), 2L, new BigDecimal("10")));

        assertThat(r.productos()).extracting(f -> f.producto().id()).containsExactly(1L, 2L);
        assertThat(r.valorTotal()).isEqualByComparingTo("100");
        assertThat(r.productos().get(0).valorConsumo()).isEqualByComparingTo("50");
    }

    @Test
    void resumen_cuentaProductosYValorPorClase() {
        AbcResponse r = clasificar(
                List.of(Datos.producto(1, "1"), Datos.producto(2, "1"), Datos.producto(3, "1"), Datos.producto(4, "1")),
                Map.of(1L, new BigDecimal("70"), 2L, new BigDecimal("20"), 3L, new BigDecimal("6"),
                        4L, new BigDecimal("4")));

        assertThat(r.resumen()).extracting(c -> c.clase().name() + ":" + c.productos())
                .containsExactly("A:2", "B:1", "C:1");
        assertThat(r.resumen().get(0).porcentajeValor()).isEqualByComparingTo("90");
    }
}
