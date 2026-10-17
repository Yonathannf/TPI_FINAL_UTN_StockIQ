package com.stockiq.security;

import com.stockiq.usuario.model.Rol;

/**
 * Usuario autenticado en el pedido en curso. Los controllers lo reciben con
 * {@code @AuthenticationPrincipal}; el usuario responsable de un movimiento sale de acá, nunca del cuerpo.
 */
public record UsuarioActual(Long id, String username, Rol rol) {
}
