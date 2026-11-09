package com.stockiq.common.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Conversión entre los días de Argentina, que usa la API en los filtros, y los instantes en UTC, que se
 * guardan en la base (RN-41, D-16).
 */
public final class Fechas {

    public static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private Fechas() {
    }

    /** Día de hoy en Argentina. */
    public static LocalDate hoy(Clock clock) {
        return LocalDate.now(clock.withZone(ZONA_ARGENTINA));
    }

    /** Instante actual en UTC, sin fracciones de segundo. */
    public static LocalDateTime ahoraUtc(Clock clock) {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC)).withNano(0);
    }

    /** Las 00:00 de un día de Argentina, expresadas en UTC. */
    public static LocalDateTime inicioDelDiaUtc(LocalDate dia) {
        return dia.atStartOfDay(ZONA_ARGENTINA).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** Día de Argentina al que corresponde un instante guardado en UTC. */
    public static LocalDate diaEnArgentina(LocalDateTime instanteUtc) {
        return instanteUtc.atZone(ZoneOffset.UTC).withZoneSameInstant(ZONA_ARGENTINA).toLocalDate();
    }
}
