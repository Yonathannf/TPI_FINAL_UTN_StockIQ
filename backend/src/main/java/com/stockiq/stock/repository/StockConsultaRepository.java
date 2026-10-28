package com.stockiq.stock.repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
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

    /**
     * RN-16: stock distinto de cero por depósito (con los datos del depósito), ordenado por nombre de
     * depósito. Con {@code productoId} null trae todos los productos.
     */
    public List<StockDepositoFila> stockPorDeposito(Long productoId) {
        String sql = """
                SELECT s.producto_id, s.deposito_id, d.nombre, d.activo, s.stock
                  FROM v_stock_deposito s
                  JOIN deposito d ON d.id = s.deposito_id
                 WHERE s.stock <> 0"""
                + (productoId == null ? "" : " AND s.producto_id = :productoId")
                + " ORDER BY d.nombre";
        Map<String, Object> params = productoId == null ? Map.of() : Map.of("productoId", productoId);
        return jdbc.query(sql, params, (rs, i) -> new StockDepositoFila(
                rs.getLong("producto_id"), rs.getLong("deposito_id"), rs.getString("nombre"),
                rs.getBoolean("activo"), rs.getBigDecimal("stock")));
    }

    /** RN-17: stock en tránsito (transferencias pendientes) por producto; solo productos con tránsito. */
    public Map<Long, BigDecimal> enTransitoPorProducto(Long productoId) {
        String sql = "SELECT producto_id, en_transito FROM v_stock_transito WHERE en_transito <> 0"
                + (productoId == null ? "" : " AND producto_id = :productoId");
        Map<String, Object> params = productoId == null ? Map.of() : Map.of("productoId", productoId);
        Map<Long, BigDecimal> resultado = new HashMap<>();
        jdbc.query(sql, params, rs -> {
            resultado.put(rs.getLong("producto_id"), rs.getBigDecimal("en_transito"));
        });
        return resultado;
    }

    /** Stock de un producto en un depósito, con los datos del depósito. */
    public record StockDepositoFila(Long productoId, Long depositoId, String depositoNombre,
                                    boolean depositoActivo, BigDecimal stock) {
    }

    private boolean existe(String sql, Long id) {
        Boolean resultado = jdbc.queryForObject(sql, Map.of("id", id), Boolean.class);
        return Boolean.TRUE.equals(resultado);
    }
}
