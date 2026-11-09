package com.stockiq.analitica.dto;

import java.math.BigDecimal;

public record RotacionProductoResponse(
        ProductoRef producto,
        CategoriaRef categoria,
        BigDecimal salidas,
        BigDecimal stockInicial,
        BigDecimal stockFinal,
        BigDecimal stockPromedio,
        BigDecimal rotacion
) {
}
