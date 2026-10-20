package com.stockiq.catalogo.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @NotBlank(message = "El código es obligatorio.")
        @Size(max = 50, message = "El código puede tener hasta 50 caracteres.")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 150, message = "El nombre puede tener hasta 150 caracteres.")
        String nombre,

        @NotNull(message = "La categoría es obligatoria.")
        Long categoriaId,

        @NotNull(message = "El proveedor es obligatorio.")
        Long proveedorId,

        @NotBlank(message = "La unidad de medida es obligatoria.")
        @Size(max = 20, message = "La unidad de medida puede tener hasta 20 caracteres.")
        String unidadMedida,

        /* RN-32 */
        @NotNull(message = "El precio de referencia es obligatorio.")
        @DecimalMin(value = "0.00", message = "El precio de referencia no puede ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 enteros y 2 decimales.")
        BigDecimal precioReferencia,

        /* RN-32 */
        @NotNull(message = "El stock de seguridad es obligatorio.")
        @DecimalMin(value = "0.00", message = "El stock de seguridad no puede ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "El stock de seguridad admite hasta 10 enteros y 2 decimales.")
        BigDecimal stockSeguridad,

        @NotNull(message = "Indicá si el producto admite cantidades fraccionarias.")
        Boolean fraccionable
) {
}
