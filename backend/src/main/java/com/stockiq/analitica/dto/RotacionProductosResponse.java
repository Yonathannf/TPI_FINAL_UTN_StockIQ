package com.stockiq.analitica.dto;

import java.time.LocalDate;
import java.util.List;

/** RF-20: rotación por producto en el período [desde, hasta] (días de Argentina, inclusivos). */
public record RotacionProductosResponse(LocalDate desde, LocalDate hasta, List<RotacionProductoResponse> productos) {
}
