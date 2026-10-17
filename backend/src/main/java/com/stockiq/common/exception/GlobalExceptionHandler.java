package com.stockiq.common.exception;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Manejador global de excepciones. Todas las respuestas de error usan el formato {@link ApiError}.
 * Extiende {@link ResponseEntityExceptionHandler} para que también las excepciones estándar de
 * Spring MVC (405, 415, parámetros faltantes, etc.) salgan con ese mismo formato.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Código de los 400 por datos inválidos; el front lo usa junto con el mapa {@code errores}. */
    public static final String CODIGO_VALIDACION = "VALIDACION";

    // ---------------------------------------------------------------- negocio

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return respuesta(ex.getStatus(), ex.getCodigo(), ex.getMessage(), request.getRequestURI());
    }

    // ------------------------------------------------------------ validaciones

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new TreeMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return objetoConErrores(HttpStatus.BAD_REQUEST, CODIGO_VALIDACION, resumen(errores), request, errores);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        Map<String, String> errores = new TreeMap<>();
        ex.getConstraintViolations().forEach(violacion ->
                errores.putIfAbsent(violacion.getPropertyPath().toString(), violacion.getMessage()));
        return respuestaConErrores(HttpStatus.BAD_REQUEST, CODIGO_VALIDACION, resumen(errores),
                request.getRequestURI(), errores);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // RN-41: el cliente no puede mandar campos que la API no define (por ejemplo `fecha`).
        if (ex.getCause() instanceof UnrecognizedPropertyException desconocida) {
            return objeto(HttpStatus.BAD_REQUEST, "CAMPO_NO_PERMITIDO",
                    "El campo '" + desconocida.getPropertyName() + "' no se admite en este pedido.", request);
        }
        return objeto(HttpStatus.BAD_REQUEST, "CUERPO_INVALIDO",
                "El cuerpo del pedido no es un JSON válido o tiene valores con formato incorrecto.", request);
    }

    // ---------------------------------------------------- base de datos / acceso

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegridad(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        log.warn("Violación de integridad de datos en {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return respuesta(HttpStatus.CONFLICT, "INTEGRIDAD_DE_DATOS",
                "La operación viola una restricción de integridad de los datos.", request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                "No tenés permiso para realizar esta operación.", request.getRequestURI());
    }

    // ---------------------------------------------------------------- resto

    /** Excepciones estándar de Spring MVC (405, 415, 404 de ruta, parámetros faltantes, etc.). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        return objeto(statusCode, codigoGenerico(statusCode), mensajeGenerico(statusCode), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleInesperada(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrió un error inesperado. Probá de nuevo más tarde.", request.getRequestURI());
    }

    // --------------------------------------------------------------- helpers

    private static String codigoGenerico(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "SOLICITUD_INVALIDA";
            case 404 -> "RECURSO_NO_ENCONTRADO";
            case 405 -> "METODO_NO_PERMITIDO";
            case 415 -> "TIPO_DE_CONTENIDO_NO_SOPORTADO";
            default -> "ERROR";
        };
    }

    private static String mensajeGenerico(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "El pedido no es válido.";
            case 404 -> "El recurso solicitado no existe.";
            case 405 -> "El método HTTP no está permitido para esta ruta.";
            case 415 -> "El tipo de contenido del pedido no está soportado.";
            default -> "No se pudo procesar el pedido.";
        };
    }

    /** "campo: mensaje; otro: mensaje", para mostrar de corrido cuando el front no marca cada campo. */
    private static String resumen(Map<String, String> errores) {
        if (errores.isEmpty()) {
            return "Los datos enviados no son válidos.";
        }
        return errores.entrySet().stream()
                .map(entrada -> entrada.getKey() + ": " + entrada.getValue())
                .collect(Collectors.joining("; "));
    }

    private static ResponseEntity<ApiError> respuestaConErrores(HttpStatusCode status, String codigo,
            String mensaje, String ruta, Map<String, String> errores) {
        return ResponseEntity.status(status).body(ApiError.conErrores(status, codigo, mensaje, ruta, errores));
    }

    private static ResponseEntity<Object> objetoConErrores(HttpStatusCode status, String codigo, String mensaje,
            WebRequest request, Map<String, String> errores) {
        String ruta = request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : "";
        return ResponseEntity.status(status).body(ApiError.conErrores(status, codigo, mensaje, ruta, errores));
    }

    private static ResponseEntity<ApiError> respuesta(HttpStatusCode status, String codigo, String mensaje,
            String ruta) {
        return ResponseEntity.status(status).body(ApiError.of(status, codigo, mensaje, ruta));
    }

    private static ResponseEntity<Object> objeto(HttpStatusCode status, String codigo, String mensaje,
            WebRequest request) {
        String ruta = request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : "";
        return ResponseEntity.status(status).body(ApiError.of(status, codigo, mensaje, ruta));
    }
}
