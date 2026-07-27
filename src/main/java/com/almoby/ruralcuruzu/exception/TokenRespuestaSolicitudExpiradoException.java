package com.almoby.ruralcuruzu.exception;

/**
 * Se lanza cuando el token para responder una observación existe y no fue
 * usado, pero ya venció su ventana de validez.
 */
public class TokenRespuestaSolicitudExpiradoException extends RuntimeException {

    public TokenRespuestaSolicitudExpiradoException() {
        super("El enlace expiró. Contactá a la cooperativa para que te manden uno nuevo");
    }
}
