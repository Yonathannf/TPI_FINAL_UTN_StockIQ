package com.stockiq.usuario.dto;

import com.stockiq.usuario.model.Rol;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta de usuario (POST /usuarios). */
public record UsuarioRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 60, message = "no puede superar los 60 caracteres")
        String username,

        @NotBlank(message = "es obligatoria")
        @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "no puede superar los 100 caracteres")
        String apellido,

        @NotNull(message = "es obligatorio")
        Rol rol) {
}
