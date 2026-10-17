package com.stockiq.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Base de las excepciones de negocio. Cada una conoce el código HTTP y el código
 * funcional (por ejemplo STOCK_INSUFICIENTE) con que se responde al cliente.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public BusinessException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }
}
