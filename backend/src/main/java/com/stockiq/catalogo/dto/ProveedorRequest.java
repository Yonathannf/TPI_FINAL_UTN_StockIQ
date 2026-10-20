package com.stockiq.catalogo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 150, message = "El nombre puede tener hasta 150 caracteres.")
        String nombre,

        @Size(max = 150, message = "El contacto puede tener hasta 150 caracteres.")
        String contacto,

        @Size(max = 50, message = "El teléfono puede tener hasta 50 caracteres.")
        String telefono,

        @Email(message = "El email no tiene un formato válido.")
        @Size(max = 150, message = "El email puede tener hasta 150 caracteres.")
        String email,

        @NotNull(message = "El lead time es obligatorio.")
        @Min(value = 0, message = "El lead time no puede ser negativo.")
        @Max(value = 365, message = "El lead time puede ser de hasta 365 días.")
        Integer leadTimeDias
) {
}
