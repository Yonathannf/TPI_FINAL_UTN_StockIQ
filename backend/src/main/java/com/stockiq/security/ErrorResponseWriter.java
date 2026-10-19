package com.stockiq.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockiq.common.exception.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Escribe un {@link ApiError} directamente en la respuesta. Los errores 401 y 403 que se producen en la
 * cadena de filtros de seguridad no pasan por el GlobalExceptionHandler, así que se resuelven acá.
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
            String codigo, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                ApiError.of(status, codigo, mensaje, request.getRequestURI()));
    }
}
