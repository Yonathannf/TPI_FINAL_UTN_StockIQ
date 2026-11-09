package com.stockiq.analitica.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.stockiq.common.exception.BadRequestException;
import com.stockiq.common.util.Fechas;

/**
 * Período de análisis en días de Argentina, inclusivos (RN-41). {@code inicio} y {@code fin} son los mismos
 * límites en UTC: [inicio, fin).
 */
public record Periodo(LocalDate desde, LocalDate hasta, LocalDateTime inicio, LocalDateTime fin) {

    /**
     * Si falta {@code hasta}, es hoy; si falta {@code desde}, son los {@code diasPorDefecto} días que
     * terminan en {@code hasta} (incluido).
     */
    public static Periodo de(LocalDate desde, LocalDate hasta, int diasPorDefecto, Clock clock) {
        LocalDate h = hasta != null ? hasta : Fechas.hoy(clock);
        LocalDate d = desde != null ? desde : h.minusDays(diasPorDefecto - 1L);
        if (d.isAfter(h)) {
            throw new BadRequestException("RANGO_FECHAS_INVALIDO", "La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
        return new Periodo(d, h, Fechas.inicioDelDiaUtc(d), Fechas.inicioDelDiaUtc(h.plusDays(1)));
    }
}
