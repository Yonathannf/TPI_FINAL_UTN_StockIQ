package com.stockiq.stock.dto;

/** Producto anidado en GET /stock/transito. */
public record ProductoTransitoResponse(Long id, String codigo, String nombre, String unidadMedida) {
}
