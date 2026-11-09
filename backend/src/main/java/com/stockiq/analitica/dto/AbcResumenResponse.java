package com.stockiq.analitica.dto;

import java.math.BigDecimal;

/** Cantidad de productos y valor de consumo de una clase del ABC. */
public record AbcResumenResponse(ClaseAbc clase, int productos, BigDecimal valorConsumo, BigDecimal porcentajeValor) {
}
