package com.stockiq.parametro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParametroUpdateRequest(
        @NotBlank(message = "El valor es obligatorio.")
        @Size(max = 50, message = "El valor puede tener hasta 50 caracteres.")
        String valor
) {
}
