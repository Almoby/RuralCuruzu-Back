package com.almoby.ruralcuruzu.exception;

/**
 * Se lanza cuando el refresh token existe y no fue usado, pero venció.
 */
public class RefreshTokenExpiradoException extends RuntimeException {

    public RefreshTokenExpiradoException() {
        super("La sesión expiró. Volvé a iniciar sesión");
    }
}
