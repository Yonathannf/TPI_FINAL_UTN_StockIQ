package com.stockiq.analitica.dto;

import java.math.BigDecimal;

/** RF-18, RF-19: punto de reposición de un producto y, si corresponde, la alerta con la cantidad sugerida. */
public record ReposicionResponse(
        ProductoRef producto,
        ProveedorRef proveedor,
        BigDecimal consumoDiario,
        BigDecimal stockSeguridad,
        BigDecimal puntoReposicion,
        BigDecimal stockDisponible,
        BigDecimal enTransito,
        boolean alerta,
        BigDecimal cantidadSugerida
) {
}
