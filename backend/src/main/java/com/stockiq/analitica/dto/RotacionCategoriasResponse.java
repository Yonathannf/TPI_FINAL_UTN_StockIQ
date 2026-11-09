package com.stockiq.analitica.dto;

import java.time.LocalDate;
import java.util.List;

/** RF-20, RN-26: rotación por categoría en el período [desde, hasta]. */
public record RotacionCategoriasResponse(LocalDate desde, LocalDate hasta, List<RotacionCategoriaResponse> categorias) {
}
