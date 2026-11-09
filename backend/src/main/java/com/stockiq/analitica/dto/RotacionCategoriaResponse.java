package com.stockiq.analitica.dto;

import java.math.BigDecimal;

public record RotacionCategoriaResponse(
        CategoriaRef categoria,
        int productos,
        BigDecimal salidas,
        BigDecimal stockPromedio,
        BigDecimal rotacion
) {
}
