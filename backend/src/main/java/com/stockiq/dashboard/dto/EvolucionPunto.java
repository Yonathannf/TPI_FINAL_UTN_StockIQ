package com.stockiq.dashboard.dto;

import java.time.LocalDate;

/**
 * Cantidad de movimientos registrados en un período. {@code periodo} es el primer día del intervalo (el lunes
 * en SEMANA, el día 1 en MES). Se cuentan movimientos, no unidades, porque los productos tienen distintas
 * unidades de medida.
 */
public record EvolucionPunto(LocalDate periodo, long entradas, long salidas, long transferencias) {
}
