package com.stockiq.stock.dto;

/** Depósito anidado en el detalle de stock por depósito. */
public record DepositoStockResponse(Long id, String nombre, boolean activo) {
}
