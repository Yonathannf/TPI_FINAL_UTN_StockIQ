package com.stockiq.usuario.dto;

import com.stockiq.usuario.model.Rol;

public record UsuarioResponse(
        Long id,
        String username,
        String nombre,
        String apellido,
        Rol rol,
        boolean activo) {
}
