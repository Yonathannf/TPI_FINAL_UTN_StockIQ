package com.stockiq.usuario.dto;

import com.stockiq.usuario.model.Rol;
import com.stockiq.usuario.model.Usuario;

/** Datos del usuario logueado que necesita el front (login y GET /auth/me). Nunca incluye la clave. */
public record SesionUsuario(
        Long id,
        String username,
        String nombre,
        String apellido,
        Rol rol) {

    public static SesionUsuario de(Usuario usuario) {
        return new SesionUsuario(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getRol());
    }
}
