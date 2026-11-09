package com.stockiq.dashboard.dto;

import java.util.List;

import com.stockiq.movimiento.dto.MovimientoResponse;
import com.stockiq.stock.dto.StockProductoResponse;

/** RF-24: stock por depósito y transferencias pendientes (para todos los roles). */
public record DashboardStockResponse(List<StockProductoResponse> stock, List<MovimientoResponse> pendientes) {
}
