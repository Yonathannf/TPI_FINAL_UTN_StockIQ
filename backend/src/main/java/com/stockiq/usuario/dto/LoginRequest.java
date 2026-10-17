package com.stockiq.usuario.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "es obligatorio") String username,
        @NotBlank(message = "es obligatorio") String password) {
}
