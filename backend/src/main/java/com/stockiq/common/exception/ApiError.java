package com.stockiq.common.exception;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Formato único de las respuestas de error (docs/06-api-endpoints.md, sección 11).
 * El timestamp va en UTC, como el resto de las fechas del sistema (RN-41).
 *
 * {@code errores} solo aparece en las validaciones (400, código VALIDACION): mapa campo → mensaje,
 * para que el front marque cada campo del formulario. En el resto de los errores se omite.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String codigo,
        String mensaje,
        String ruta,
        Map<String, String> errores) {

    public static ApiError of(HttpStatusCode status, String codigo, String mensaje, String ruta) {
        return conErrores(status, codigo, mensaje, ruta, null);
    }

    public static ApiError conErrores(HttpStatusCode status, String codigo, String mensaje, String ruta,
            Map<String, String> errores) {
        HttpStatus resuelto = HttpStatus.resolve(status.value());
        String descripcion = resuelto != null ? resuelto.getReasonPhrase() : "Error";
        return new ApiError(
                LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS),
                status.value(),
                descripcion,
                codigo,
                mensaje,
                ruta,
                errores);
    }
}
