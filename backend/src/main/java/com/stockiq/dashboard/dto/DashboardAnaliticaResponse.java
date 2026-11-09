package com.stockiq.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.stockiq.analitica.dto.AbcResumenResponse;
import com.stockiq.analitica.dto.ReposicionResponse;
import com.stockiq.analitica.dto.RotacionCategoriaResponse;

/** RF-23: alertas de reposición, resumen ABC y rotación por categoría del período de análisis. */
public record DashboardAnaliticaResponse(
        LocalDate desde,
        LocalDate hasta,
        List<ReposicionResponse> alertas,
        BigDecimal valorConsumoTotal,
        List<AbcResumenResponse> resumenAbc,
        List<RotacionCategoriaResponse> rotacionCategorias
) {
}
