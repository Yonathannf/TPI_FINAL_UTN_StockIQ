package com.stockiq.movimiento.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stockiq.movimiento.model.Movimiento;

import jakarta.persistence.LockModeType;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long>, JpaSpecificationExecutor<Movimiento> {

    /** Historial paginado; trae las referencias en la misma consulta para no hacer una por movimiento. */
    @Override
    @EntityGraph(attributePaths = {"producto", "usuario", "depositoOrigen", "depositoDestino", "usuarioResolucion"})
    Page<Movimiento> findAll(Specification<Movimiento> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"producto", "usuario", "depositoOrigen", "depositoDestino", "usuarioResolucion"})
    List<Movimiento> findAll(Specification<Movimiento> spec, Sort sort);

    /** Bloquea la transferencia (SELECT ... FOR UPDATE) antes de confirmarla o cancelarla (D-13, RN-13). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Movimiento m where m.id = :id")
    Optional<Movimiento> bloquearPorId(@Param("id") Long id);
}
