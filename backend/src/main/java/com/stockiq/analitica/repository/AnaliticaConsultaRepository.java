package com.stockiq.analitica.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.stockiq.movimiento.model.TipoMovimiento;

/**
 * Agregaciones de la capa analítica, resueltas en la base con una sola consulta por indicador para todo
 * el catálogo (RNF-08). Los instantes llegan en UTC y los calcula el Service a partir del reloj (D-16):
 * las consultas no usan NOW().
 */
@Repository
public class AnaliticaConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AnaliticaConsultaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** RN-19, RN-25, RN-27: cantidad total de salidas por producto en [desde, hasta). Las transferencias no cuentan. */
    public Map<Long, BigDecimal> salidasPorProducto(LocalDateTime desde, LocalDateTime hasta) {
        return porProducto("""
                SELECT producto_id, SUM(cantidad) AS total
                  FROM movimiento
                 WHERE tipo = 'SALIDA' AND fecha >= :desde AND fecha < :hasta
                 GROUP BY producto_id""",
                Map.of("desde", desde, "hasta", hasta));
    }

    /**
     * RN-36: stock total de cada producto en un instante, sumando todos los depósitos (activos o no) y
     * considerando solo los movimientos registrados antes de ese instante. Una transferencia que en ese
     * momento estaba en tránsito (todavía pendiente, o resuelta después) descuenta del total; una ya
     * confirmada o cancelada no lo modifica.
     */
    public Map<Long, BigDecimal> stockTotalAl(LocalDateTime instante) {
        return porProducto("""
                SELECT producto_id,
                       SUM(CASE
                             WHEN tipo = 'ENTRADA' THEN cantidad
                             WHEN tipo = 'SALIDA'  THEN -cantidad
                             WHEN estado_transferencia = 'PENDIENTE' OR fecha_resolucion >= :instante THEN -cantidad
                             ELSE 0
                           END) AS total
                  FROM movimiento
                 WHERE fecha < :instante
                 GROUP BY producto_id""",
                Map.of("instante", instante));
    }

    /** RN-18: stock total disponible por producto, solo en depósitos activos y sin el tránsito. */
    public Map<Long, BigDecimal> stockDisponiblePorProducto() {
        return porProducto("""
                SELECT s.producto_id, SUM(s.stock) AS total
                  FROM v_stock_deposito s
                  JOIN deposito d ON d.id = s.deposito_id
                 WHERE d.activo = TRUE
                 GROUP BY s.producto_id""",
                Map.of());
    }

    /** RF-25: tipo y fecha (UTC) de los movimientos registrados en [desde, hasta). */
    public List<MovimientoFecha> movimientosEntre(LocalDateTime desde, LocalDateTime hasta) {
        return jdbc.query("""
                SELECT tipo, fecha FROM movimiento
                 WHERE fecha >= :desde AND fecha < :hasta""",
                Map.of("desde", desde, "hasta", hasta),
                (rs, i) -> new MovimientoFecha(TipoMovimiento.valueOf(rs.getString("tipo")),
                        rs.getObject("fecha", LocalDateTime.class)));
    }

    public record MovimientoFecha(TipoMovimiento tipo, LocalDateTime fecha) {
    }

    private Map<Long, BigDecimal> porProducto(String sql, Map<String, ?> params) {
        Map<Long, BigDecimal> resultado = new HashMap<>();
        jdbc.query(sql, params, rs -> {
            BigDecimal total = rs.getBigDecimal("total");
            resultado.put(rs.getLong("producto_id"), total == null ? BigDecimal.ZERO : total);
        });
        return resultado;
    }
}
