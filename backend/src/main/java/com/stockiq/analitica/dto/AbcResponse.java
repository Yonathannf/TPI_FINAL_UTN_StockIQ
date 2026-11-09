package com.stockiq.analitica.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** RF-21: clasificación ABC del período [desde, hasta], ordenada de mayor a menor valor de consumo. */
public record AbcResponse(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal valorTotal,
        List<AbcProductoResponse> productos,
        List<AbcResumenResponse> resumen
) {
}
