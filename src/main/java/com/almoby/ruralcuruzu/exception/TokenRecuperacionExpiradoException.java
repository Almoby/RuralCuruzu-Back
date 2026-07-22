package com.almoby.ruralcuruzu.exception;

/**
 * Se lanza cuando el token de recuperación existe y no fue usado,
 * pero ya venció su ventana de validez.
 */
public class TokenRecuperacionExpiradoException extends RuntimeException {

    public TokenRecuperacionExpiradoException() {
        super("El enlace de recuperación expiró. Solicitá uno nuevo");
    }
}
