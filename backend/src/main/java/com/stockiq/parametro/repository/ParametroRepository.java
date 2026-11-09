package com.stockiq.parametro.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stockiq.parametro.model.Parametro;

public interface ParametroRepository extends JpaRepository<Parametro, String> {
}
