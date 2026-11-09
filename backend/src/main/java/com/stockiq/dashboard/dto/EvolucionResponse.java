package com.stockiq.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

/** RF-25: evolución de los movimientos entre {@code desde} y {@code hasta}, incluidos los períodos sin movimientos. */
public record EvolucionResponse(LocalDate desde, LocalDate hasta, Agrupacion agrupacion, List<EvolucionPunto> puntos) {
}
