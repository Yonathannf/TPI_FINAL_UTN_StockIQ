package com.stockiq.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stockiq.analitica.repository.AnaliticaConsultaRepository;
import com.stockiq.analitica.repository.AnaliticaConsultaRepository.MovimientoFecha;
import com.stockiq.common.exception.BadRequestException;
import com.stockiq.dashboard.dto.Agrupacion;
import com.stockiq.dashboard.dto.EvolucionResponse;
import com.stockiq.movimiento.model.TipoMovimiento;
import com.stockiq.parametro.service.ParametroService;
import com.stockiq.parametro.service.ParametrosAnalitica;

/** RF-25: evolución agrupada por día, semana o mes, en hora de Argentina (RN-41). */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-02T15:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AnaliticaConsultaRepository consultas;
    @Mock
    private ParametroService parametroService;

    private DashboardService servicio;

    @BeforeEach
    void setUp() {
        servicio = new DashboardService(null, null, null, null, null, consultas, parametroService, RELOJ);
        when(parametroService.configuracion()).thenReturn(ParametrosAnalitica.POR_DEFECTO);
    }

    @Test
    void porDia_incluyeLosDiasSinMovimientos_yUsaLaHoraDeArgentina() {
        // 01:30 UTC del 3/10 son las 22:30 del 2/10 en Argentina: cuenta para el día 2
        when(consultas.movimientosEntre(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(
                new MovimientoFecha(TipoMovimiento.ENTRADA, LocalDateTime.of(2026, 10, 1, 15, 0)),
                new MovimientoFecha(TipoMovimiento.SALIDA, LocalDateTime.of(2026, 10, 3, 1, 30)),
                new MovimientoFecha(TipoMovimiento.SALIDA, LocalDateTime.of(2026, 10, 3, 14, 0))));

        EvolucionResponse r = servicio.evolucion(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), Agrupacion.DIA);

        assertThat(r.puntos()).hasSize(3);
        assertThat(r.puntos().get(0).entradas()).isEqualTo(1);
        assertThat(r.puntos().get(1).salidas()).isEqualTo(1);
        assertThat(r.puntos().get(2).salidas()).isEqualTo(1);
    }

    @Test
    void porSemana_agrupaDesdeElLunes() {
        when(consultas.movimientosEntre(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(
                new MovimientoFecha(TipoMovimiento.TRANSFERENCIA, LocalDateTime.of(2026, 10, 7, 15, 0)),
                new MovimientoFecha(TipoMovimiento.TRANSFERENCIA, LocalDateTime.of(2026, 10, 9, 15, 0))));

        // 1/10/2026 es jueves: la primera semana empieza el lunes 28/9
        EvolucionResponse r = servicio.evolucion(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 14), Agrupacion.SEMANA);

        assertThat(r.puntos()).extracting(p -> p.periodo().toString())
                .containsExactly("2026-09-28", "2026-10-05", "2026-10-12");
        assertThat(r.puntos().get(1).transferencias()).isEqualTo(2);
    }

    @Test
    void porMes_empiezaElDiaUno() {
        when(consultas.movimientosEntre(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());

        EvolucionResponse r = servicio.evolucion(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 10, 2), Agrupacion.MES);

        assertThat(r.puntos()).extracting(p -> p.periodo().toString())
                .containsExactly("2026-08-01", "2026-09-01", "2026-10-01");
    }

    @Test
    void sinAgrupacion_usaSemana_ySinFechas_elPeriodoDeAnalisis() {
        when(consultas.movimientosEntre(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());

        EvolucionResponse r = servicio.evolucion(null, null, null);

        assertThat(r.agrupacion()).isEqualTo(Agrupacion.SEMANA);
        assertThat(r.hasta()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(r.desde()).isEqualTo(LocalDate.of(2026, 8, 5));
    }

    @Test
    void demasiadosPuntos_responde400() {
        assertThatThrownBy(() -> servicio.evolucion(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1), Agrupacion.DIA))
                .isInstanceOf(BadRequestException.class)
                .extracting("codigo").isEqualTo("RANGO_DEMASIADO_GRANDE");
    }
}
