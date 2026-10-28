package com.stockiq.stock.dto;

import java.math.BigDecimal;

/** Stock en tránsito de un producto (RN-17). */
public record StockTransitoResponse(ProductoTransitoResponse producto, BigDecimal enTransito) {
}
