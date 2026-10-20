package com.stockiq.stock.repository;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Consultas de stock sobre las vistas v_stock_deposito y v_stock_transito y la tabla
 * movimiento (RN-15 a RN-17). En la semana 2 se usan para validar las bajas de
 * productos y depósitos; en la semana 3 este repositorio se amplía con las consultas
 * de stock por depósito, total y en tránsito.
 */
@Repository
public class StockConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public StockConsultaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** RN-38: el depósito tiene stock distinto de cero en algún producto. */
    public boolean depositoTieneStock(Long depositoId) {
        return existe("""
                SELECT EXISTS (
                  SELECT 1 FROM v_stock_deposito WHERE deposito_id = :id AND stock <> 0
                )""", depositoId);
    }

    /** RN-38: hay transferencias pendientes con origen o destino en el depósito. */
    public boolean depositoTieneTransferenciasPendientes(Long depositoId) {
        return existe("""
                SELECT EXISTS (
                  SELECT 1 FROM movimiento
                   WHERE tipo = 'TRANSFERENCIA' AND estado_transferencia = 'PENDIENTE'
                     AND (deposito_origen_id = :id OR deposito_destino_id = :id)
                )""", depositoId);
    }

    /** RN-39: el producto tiene stock distinto de cero en algún depósito. */
    public boolean productoTieneStock(Long productoId) {
        return existe("""
                SELECT EXISTS (
                  SELECT 1 FROM v_stock_deposito WHERE producto_id = :id AND stock <> 0
                )""", productoId);
    }

    /** RN-39: el producto tiene transferencias pendientes (stock en tránsito). */
    public boolean productoTieneTransferenciasPendientes(Long productoId) {
        return existe("""
                SELECT EXISTS (
                  SELECT 1 FROM movimiento
                   WHERE producto_id = :id
                     AND tipo = 'TRANSFERENCIA' AND estado_transferencia = 'PENDIENTE'
                )""", productoId);
    }

    /** RN-47: el stock en algún depósito o el stock en tránsito no es un número entero. */
    public boolean productoTieneStockNoEntero(Long productoId) {
        return existe("""
                SELECT EXISTS (
                  SELECT 1 FROM v_stock_deposito WHERE producto_id = :id AND stock <> FLOOR(stock)
                  UNION ALL
                  SELECT 1 FROM v_stock_transito WHERE producto_id = :id AND en_transito <> FLOOR(en_transito)
                )""", productoId);
    }

    private boolean existe(String sql, Long id) {
        Boolean resultado = jdbc.queryForObject(sql, Map.of("id", id), Boolean.class);
        return Boolean.TRUE.equals(resultado);
    }
}
