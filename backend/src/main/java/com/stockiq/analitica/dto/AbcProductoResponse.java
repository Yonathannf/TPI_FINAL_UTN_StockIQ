package com.stockiq.analitica.dto;

import java.math.BigDecimal;

/**
 * RN-27: {@code porcentaje} es la participación del producto en el valor total y
 * {@code porcentajeAcumulado} incluye a este producto y a todos los anteriores del ranking.
 */
public record AbcProductoResponse(
        ProductoRef producto,
        CategoriaRef categoria,
        BigDecimal cantidadSalidas,
        BigDecimal precioReferencia,
        BigDecimal valorConsumo,
        BigDecimal porcentaje,
        BigDecimal porcentajeAcumulado,
        ClaseAbc clase
) {
}
