package com.stockiq.movimiento.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** RN-05: la transferencia tiene depósito origen y destino, distintos entre sí. */
public record TransferenciaRequest(
        @NotNull(message = "El producto es obligatorio.")
        Long productoId,

        @NotNull(message = "El depósito de origen es obligatorio.")
        Long depositoOrigenId,

        @NotNull(message = "El depósito de destino es obligatorio.")
        Long depositoDestinoId,

        @NotNull(message = "La cantidad es obligatoria.")
        @Positive(message = "La cantidad debe ser mayor a cero.")
        @Digits(integer = 10, fraction = 2, message = "La cantidad admite hasta 10 enteros y 2 decimales.")
        BigDecimal cantidad,

        @Size(max = 255, message = "El motivo no puede superar los 255 caracteres.")
        String motivo
) {
}
