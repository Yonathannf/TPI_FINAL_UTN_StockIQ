package com.stockiq.common.exception;

import org.springframework.http.HttpStatus;

/** 404: el recurso no existe. */
public class NotFoundException extends BusinessException {

    public static final String CODIGO = "RECURSO_NO_ENCONTRADO";

    public NotFoundException(String mensaje) {
        super(HttpStatus.NOT_FOUND, CODIGO, mensaje);
    }

    public static NotFoundException of(String recurso, Object id) {
        return new NotFoundException(recurso + " con id " + id + " no encontrado.");
    }
}
