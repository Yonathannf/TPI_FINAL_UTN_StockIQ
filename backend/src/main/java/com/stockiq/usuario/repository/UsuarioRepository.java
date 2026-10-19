package com.stockiq.usuario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stockiq.usuario.model.Rol;
import com.stockiq.usuario.model.Usuario;

import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Usuario> findByActivo(boolean activo, Sort sort);

    /**
     * RN-40: bloquea (SELECT ... FOR UPDATE) a los usuarios activos con el rol indicado, siempre en el
     * mismo orden para evitar deadlocks. Así dos bajas simultáneas no pueden dejar el sistema sin ENCARGADO.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.rol = :rol and u.activo = true order by u.id")
    List<Usuario> bloquearActivosPorRol(@Param("rol") Rol rol);
}
