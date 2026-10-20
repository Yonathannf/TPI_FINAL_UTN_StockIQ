package com.stockiq.deposito.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stockiq.deposito.model.Deposito;

import jakarta.persistence.LockModeType;

public interface DepositoRepository extends JpaRepository<Deposito, Long> {

    List<Deposito> findByActivo(boolean activo, Sort sort);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    /** Bloquea la fila del depósito antes de validar RN-38 (ver D-13). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Deposito d where d.id = :id")
    Optional<Deposito> bloquearPorId(@Param("id") Long id);
}
