package com.stockiq.parametro.service;

import java.math.BigDecimal;

/** Valores de los parámetros ya convertidos y validados, listos para usar en los cálculos (RN-37). */
public record ParametrosAnalitica(
        int ventanaConsumoDias,
        int diasCobertura,
        int periodoAnalisisDias,
        BigDecimal corteAbcA,
        BigDecimal corteAbcB
) {

    /** Valores por defecto de la migración V1 (02-reglas-de-negocio.md, sección 6). */
    public static final ParametrosAnalitica POR_DEFECTO =
            new ParametrosAnalitica(30, 30, 90, new BigDecimal("80"), new BigDecimal("95"));
}
