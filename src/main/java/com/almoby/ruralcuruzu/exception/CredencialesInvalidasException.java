package com.almoby.ruralcuruzu.exception;

/**
 * Se lanza cuando el email no existe o la contraseña no coincide.
 * El mensaje es intencionalmente genérico (no distingue "usuario no existe"
 * de "contraseña incorrecta") para no filtrar qué emails están registrados.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }
}
