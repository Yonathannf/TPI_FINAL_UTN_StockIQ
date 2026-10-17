package com.stockiq.usuario.dto;

import com.stockiq.usuario.model.Rol;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edición de usuario (PUT /usuarios/{id}). El username no se modifica; la contraseña es opcional. */
public record UsuarioUpdateRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "no puede superar los 100 caracteres")
        String apellido,

        @NotNull(message = "es obligatorio")
        Rol rol,

        /** Si no viene (null), la contraseña actual no se toca. */
        @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres")
        String password) {
}
