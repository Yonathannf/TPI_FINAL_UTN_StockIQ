package com.stockiq.catalogo.dto;

public record ProveedorResponse(
        Long id,
        String nombre,
        String contacto,
        String telefono,
        String email,
        int leadTimeDias,
        boolean activo
) {
}
