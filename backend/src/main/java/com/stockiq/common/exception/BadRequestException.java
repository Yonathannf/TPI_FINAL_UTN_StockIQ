package com.stockiq.common.exception;

import org.springframework.http.HttpStatus;

/** 400: datos inválidos que no alcanza a detectar la validación declarativa del DTO. */
public class BadRequestException extends BusinessException {

    public BadRequestException(String codigo, String mensaje) {
        super(HttpStatus.BAD_REQUEST, codigo, mensaje);
    }
}
