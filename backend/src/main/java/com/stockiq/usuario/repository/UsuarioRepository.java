package com.stockiq.usuario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.stockiq.usuario.model.Rol;
import com.stockiq.usuario.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Usuario> findByActivo(boolean activo, Sort sort);

    /** Para RN-40: cuántos usuarios activos tienen el rol indicado. */
    long countByRolAndActivoTrue(Rol rol);
}
