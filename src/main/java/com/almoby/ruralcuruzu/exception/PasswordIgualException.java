package com.almoby.ruralcuruzu.exception;

/**
 * Se lanza cuando el usuario intenta "restablecer" la contraseña
 * usando la misma que ya tenía.
 */
public class PasswordIgualException extends RuntimeException {

    public PasswordIgualException() {
        super("La nueva contraseña no puede ser igual a la anterior");
    }
}
