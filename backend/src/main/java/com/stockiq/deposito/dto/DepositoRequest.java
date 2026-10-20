package com.stockiq.deposito.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepositoRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre puede tener hasta 100 caracteres.")
        String nombre,

        @Size(max = 255, message = "La ubicación puede tener hasta 255 caracteres.")
        String ubicacion
) {
}
