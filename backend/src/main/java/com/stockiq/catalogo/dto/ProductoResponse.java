package com.stockiq.catalogo.dto;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        ReferenciaResponse categoria,
        ReferenciaResponse proveedor,
        String unidadMedida,
        BigDecimal precioReferencia,
        BigDecimal stockSeguridad,
        boolean fraccionable,
        boolean activo
) {
}
