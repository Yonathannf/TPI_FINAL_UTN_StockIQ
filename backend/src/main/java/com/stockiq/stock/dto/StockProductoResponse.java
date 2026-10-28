package com.stockiq.stock.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Stock de un producto. {@code totalDisponible} suma solo los depósitos activos y no incluye el
 * tránsito (RN-18); {@code enTransito} es la suma de sus transferencias pendientes (RN-17).
 */
public record StockProductoResponse(ProductoStockResponse producto,
                                    List<StockPorDepositoResponse> porDeposito,
                                    BigDecimal totalDisponible,
                                    BigDecimal enTransito) {
}
