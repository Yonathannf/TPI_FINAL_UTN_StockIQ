package com.stockiq.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 409: la operación choca con el estado actual del sistema (stock insuficiente,
 * transferencia que no está pendiente, entidad inactiva, nombre duplicado, etc.).
 */
public class ConflictException extends BusinessException {

    public ConflictException(String codigo, String mensaje) {
        super(HttpStatus.CONFLICT, codigo, mensaje);
    }
}
