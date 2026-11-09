package com.stockiq.dashboard.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockiq.analitica.dto.AbcResponse;
import com.stockiq.analitica.dto.RotacionCategoriasResponse;
import com.stockiq.analitica.repository.AnaliticaConsultaRepository;
import com.stockiq.analitica.repository.AnaliticaConsultaRepository.MovimientoFecha;
import com.stockiq.analitica.service.AbcService;
import com.stockiq.analitica.service.Periodo;
import com.stockiq.analitica.service.ReposicionService;
import com.stockiq.analitica.service.RotacionService;
import com.stockiq.common.exception.BadRequestException;
import com.stockiq.common.util.Fechas;
import com.stockiq.dashboard.dto.Agrupacion;
import com.stockiq.dashboard.dto.DashboardAnaliticaResponse;
import com.stockiq.dashboard.dto.DashboardStockResponse;
import com.stockiq.dashboard.dto.EvolucionPunto;
import com.stockiq.dashboard.dto.EvolucionResponse;
import com.stockiq.movimiento.model.TipoMovimiento;
import com.stockiq.movimiento.service.MovimientoService;
import com.stockiq.parametro.service.ParametroService;
import com.stockiq.stock.service.StockService;

import lombok.RequiredArgsConstructor;

/** RF-23 a RF-25: combina los indicadores para las pantallas del dashboard. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** Tope de puntos de la evolución, para que el gráfico siga siendo legible y la respuesta liviana. */
    static final int MAX_PUNTOS = 400;

    private final ReposicionService reposicionService;
    private final AbcService abcService;
    private final RotacionService rotacionService;
    private final StockService stockService;
    private final MovimientoService movimientoService;
    private final AnaliticaConsultaRepository consultas;
    private final ParametroService parametroService;
    private final Clock clock;

    /** RF-23 (solo ENCARGADO). */
    @Transactional(readOnly = true)
    public DashboardAnaliticaResponse analitica() {
        AbcResponse abc = abcService.calcular();
        RotacionCategoriasResponse rotacion = rotacionService.porCategoria(abc.desde(), abc.hasta());
        return new DashboardAnaliticaResponse(abc.desde(), abc.hasta(), reposicionService.calcular(true),
                abc.valorTotal(), abc.resumen(), rotacion.categorias());
    }

    /** RF-24 (todos los roles). */
    @Transactional(readOnly = true)
    public DashboardStockResponse stock() {
        return new DashboardStockResponse(stockService.listar(null, null), movimientoService.pendientes(null));
    }

    /** RF-25 (solo ENCARGADO). Por defecto, el período de análisis agrupado por semana. */
    @Transactional(readOnly = true)
    public EvolucionResponse evolucion(LocalDate desde, LocalDate hasta, Agrupacion agrupacion) {
        Periodo periodo = Periodo.de(desde, hasta, parametroService.configuracion().periodoAnalisisDias(), clock);
        Agrupacion grupo = agrupacion == null ? Agrupacion.SEMANA : agrupacion;

        // Todos los intervalos del período, aunque no tengan movimientos, para que el gráfico no tenga huecos
        Map<LocalDate, EnumMap<TipoMovimiento, Long>> conteos = new LinkedHashMap<>();
        LocalDate ultimo = inicioDelIntervalo(periodo.hasta(), grupo);
        for (LocalDate d = inicioDelIntervalo(periodo.desde(), grupo); !d.isAfter(ultimo); d = siguiente(d, grupo)) {
            if (conteos.size() >= MAX_PUNTOS) {
                throw new BadRequestException("RANGO_DEMASIADO_GRANDE",
                        "El período tiene demasiados puntos para esa agrupación. Elegí una agrupación mayor.");
            }
            conteos.put(d, new EnumMap<>(TipoMovimiento.class));
        }

        for (MovimientoFecha m : consultas.movimientosEntre(periodo.inicio(), periodo.fin())) {
            LocalDate clave = inicioDelIntervalo(Fechas.diaEnArgentina(m.fecha()), grupo);
            EnumMap<TipoMovimiento, Long> conteo = conteos.get(clave);
            if (conteo != null) {
                conteo.merge(m.tipo(), 1L, Long::sum);
            }
        }

        List<EvolucionPunto> puntos = new ArrayList<>();
        conteos.forEach((dia, c) -> puntos.add(new EvolucionPunto(dia,
                c.getOrDefault(TipoMovimiento.ENTRADA, 0L),
                c.getOrDefault(TipoMovimiento.SALIDA, 0L),
                c.getOrDefault(TipoMovimiento.TRANSFERENCIA, 0L))));
        return new EvolucionResponse(periodo.desde(), periodo.hasta(), grupo, puntos);
    }

    static LocalDate inicioDelIntervalo(LocalDate dia, Agrupacion agrupacion) {
        return switch (agrupacion) {
            case DIA -> dia;
            case SEMANA -> dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> dia.withDayOfMonth(1);
        };
    }

    private static LocalDate siguiente(LocalDate dia, Agrupacion agrupacion) {
        return switch (agrupacion) {
            case DIA -> dia.plusDays(1);
            case SEMANA -> dia.plusWeeks(1);
            case MES -> dia.plusMonths(1);
        };
    }
}
