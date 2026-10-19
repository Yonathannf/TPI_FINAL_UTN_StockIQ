package com.stockiq.common.exception;

import org.springframework.http.HttpStatus;

/** 401: credenciales incorrectas en el login. */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String codigo, String mensaje) {
        super(HttpStatus.UNAUTHORIZED, codigo, mensaje);
    }

    /** Mismo mensaje para usuario inexistente, inactivo o clave incorrecta, para no dar pistas. */
    public static UnauthorizedException credencialesInvalidas() {
        return new UnauthorizedException("CREDENCIALES_INVALIDAS", "Usuario o contraseña incorrectos.");
    }
}
