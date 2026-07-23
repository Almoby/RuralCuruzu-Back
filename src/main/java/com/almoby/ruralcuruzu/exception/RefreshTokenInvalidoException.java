package com.almoby.ruralcuruzu.exception;

/**
 * Se lanza cuando el refresh token no existe, ya fue usado (rotado) o
 * corresponde a un usuario que ya no existe. Mensaje genérico: el cliente
 * debe simplemente volver a loguearse, sin más detalle.
 */
public class RefreshTokenInvalidoException extends RuntimeException {

    public RefreshTokenInvalidoException() {
        super("La sesión no es válida. Volvé a iniciar sesión");
    }
}
