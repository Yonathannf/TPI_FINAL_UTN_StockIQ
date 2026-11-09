package com.stockiq.analitica.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stockiq.analitica.dto.RotacionCategoriasResponse;
import com.stockiq.analitica.dto.RotacionProductosResponse;
import com.stockiq.analitica.repository.AnaliticaConsultaRepository;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.common.exception.BadRequestException;
import com.stockiq.parametro.service.ParametroService;
import com.stockiq.parametro.service.ParametrosAnalitica;

@ExtendWith(MockitoExtension.class)
class RotacionServiceTest {

    // 2 de noviembre a las 12:00 de Argentina
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-02T15:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private AnaliticaConsultaRepository consultas;
    @Mock
    private ParametroService parametroService;

    private RotacionService servicio;

    @BeforeEach
    void setUp() {
        servicio = new RotacionService(productoRepository, consultas, parametroService, RELOJ);
    }

    @Test
    void rotacion_conStockPromedioCero_esCero_RN25() {
        assertThat(RotacionService.rotacion(new BigDecimal("10"), BigDecimal.ZERO)).isEqualByComparingTo("0");
    }

    @Test
    void periodoPorDefecto_sonLosUltimosDiasDeAnalisis_terminandoHoyEnArgentina() {
        when(parametroService.configuracion()).thenReturn(ParametrosAnalitica.POR_DEFECTO);

        Periodo periodo = servicio.periodo(null, null);

        assertThat(periodo.hasta()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(periodo.desde()).isEqualTo(LocalDate.of(2026, 8, 5)); // 90 días, incluido hoy
        // 00:00 de Argentina = 03:00 UTC
        assertThat(periodo.inicio()).isEqualTo(LocalDateTime.of(2026, 8, 5, 3, 0));
        assertThat(periodo.fin()).isEqualTo(LocalDateTime.of(2026, 11, 3, 3, 0));
    }

    @Test
    void desdePosteriorAHasta_responde400() {
        when(parametroService.configuracion()).thenReturn(ParametrosAnalitica.POR_DEFECTO);

        assertThatThrownBy(() -> servicio.porProducto(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1)))
                .isInstanceOf(BadRequestException.class)
                .extracting("codigo").isEqualTo("RANGO_FECHAS_INVALIDO");
    }

    @Test
    void porProducto_divideLasSalidasPorElStockPromedio_RN25() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);
        when(parametroService.configuracion()).thenReturn(ParametrosAnalitica.POR_DEFECTO);
        when(productoRepository.buscar(null, null, true, null)).thenReturn(List.of(Datos.producto(1, "10")));
        when(consultas.salidasPorProducto(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Map.of(1L, new BigDecimal("120")));
        // stock inicial 50 y final 30 → promedio 40 → rotación 3
        when(consultas.stockTotalAl(eq(LocalDateTime.of(2026, 10, 1, 3, 0)))).thenReturn(Map.of(1L, new BigDecimal("50")));
        when(consultas.stockTotalAl(eq(LocalDateTime.of(2026, 11, 1, 3, 0)))).thenReturn(Map.of(1L, new BigDecimal("30")));

        RotacionProductosResponse r = servicio.porProducto(desde, hasta);

        assertThat(r.productos()).hasSize(1);
        assertThat(r.productos().get(0).stockPromedio()).isEqualByComparingTo("40");
        assertThat(r.productos().get(0).rotacion()).isEqualByComparingTo("3");
    }

    @Test
    void porCategoria_sumaSalidasYStockPromedioDeSusProductos_RN26() {
        when(parametroService.configuracion()).thenReturn(ParametrosAnalitica.POR_DEFECTO);
        when(productoRepository.buscar(null, null, true, null)).thenReturn(List.of(
                Datos.producto(1, "10", 5, "0", false, Datos.BEBIDAS),
                Datos.producto(2, "10", 5, "0", false, Datos.BEBIDAS),
                Datos.producto(3, "10", 5, "0", false, Datos.SNACKS)));
        when(consultas.salidasPorProducto(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Map.of(1L, new BigDecimal("30"), 2L, new BigDecimal("10")));
        // promedios: producto 1 → 10, producto 2 → 10, producto 3 → 5
        when(consultas.stockTotalAl(any(LocalDateTime.class)))
                .thenReturn(Map.of(1L, new BigDecimal("10"), 2L, new BigDecimal("10"), 3L, new BigDecimal("5")));

        RotacionCategoriasResponse r = servicio.porCategoria(null, null);

        // Bebidas: 40 / 20 = 2; Snacks (inactiva, con un producto activo, RN-43): 0 / 5 = 0
        assertThat(r.categorias()).extracting(c -> c.categoria().nombre()).containsExactly("Bebidas", "Snacks");
        assertThat(r.categorias().get(0).rotacion()).isEqualByComparingTo("2");
        assertThat(r.categorias().get(0).productos()).isEqualTo(2);
        assertThat(r.categorias().get(1).categoria().activo()).isFalse();
    }
}
