package com.stockiq.movimiento.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.stockiq.movimiento.model.EstadoTransferencia;
import com.stockiq.movimiento.model.Movimiento;
import com.stockiq.movimiento.model.TipoMovimiento;

/** Filtros opcionales del historial (RF-15); un filtro null no se aplica. */
public final class MovimientoSpecs {

    private MovimientoSpecs() {
    }

    public static Specification<Movimiento> producto(Long productoId) {
        return (m, q, cb) -> productoId == null ? null : cb.equal(m.get("producto").get("id"), productoId);
    }

    /** El depósito puede ser el origen o el destino del movimiento. */
    public static Specification<Movimiento> deposito(Long depositoId) {
        return (m, q, cb) -> depositoId == null ? null : cb.or(
                cb.equal(m.get("depositoOrigen").get("id"), depositoId),
                cb.equal(m.get("depositoDestino").get("id"), depositoId));
    }

    public static Specification<Movimiento> tipo(TipoMovimiento tipo) {
        return (m, q, cb) -> tipo == null ? null : cb.equal(m.get("tipo"), tipo);
    }

    public static Specification<Movimiento> usuario(Long usuarioId) {
        return (m, q, cb) -> usuarioId == null ? null : cb.equal(m.get("usuario").get("id"), usuarioId);
    }

    /** Desde (inclusive), en UTC. */
    public static Specification<Movimiento> desde(LocalDateTime desde) {
        return (m, q, cb) -> desde == null ? null : cb.greaterThanOrEqualTo(m.get("fecha"), desde);
    }

    /** Hasta (exclusive), en UTC. */
    public static Specification<Movimiento> antesDe(LocalDateTime limite) {
        return (m, q, cb) -> limite == null ? null : cb.lessThan(m.get("fecha"), limite);
    }

    public static Specification<Movimiento> transferenciasPendientes() {
        return (m, q, cb) -> cb.and(
                cb.equal(m.get("tipo"), TipoMovimiento.TRANSFERENCIA),
                cb.equal(m.get("estadoTransferencia"), EstadoTransferencia.PENDIENTE));
    }

    public static Specification<Movimiento> destino(Long depositoDestinoId) {
        return (m, q, cb) -> depositoDestinoId == null ? null
                : cb.equal(m.get("depositoDestino").get("id"), depositoDestinoId);
    }
}
