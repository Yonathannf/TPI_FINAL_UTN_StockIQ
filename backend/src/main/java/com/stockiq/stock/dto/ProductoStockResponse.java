package com.stockiq.stock.dto;

/** Producto anidado en las respuestas de stock (GET /stock y GET /stock/productos/{id}). */
public record ProductoStockResponse(Long id, String codigo, String nombre, String unidadMedida,
                                    boolean fraccionable) {
}
