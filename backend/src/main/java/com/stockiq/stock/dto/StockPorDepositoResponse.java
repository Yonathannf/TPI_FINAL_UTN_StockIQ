package com.stockiq.stock.dto;

import java.math.BigDecimal;

/** Stock de un producto en un depósito (RN-16). Solo se informan los depósitos con stock distinto de cero. */
public record StockPorDepositoResponse(DepositoStockResponse deposito, BigDecimal cantidad) {
}
