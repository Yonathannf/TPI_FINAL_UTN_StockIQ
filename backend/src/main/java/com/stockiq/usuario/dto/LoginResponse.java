package com.stockiq.usuario.dto;

/** RF-01: token JWT y datos del usuario ({@code usuario}: id, username, nombre, apellido, rol). */
public record LoginResponse(
        String token,
        SesionUsuario usuario) {
}
