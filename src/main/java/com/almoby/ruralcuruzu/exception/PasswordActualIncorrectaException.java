package com.almoby.ruralcuruzu.exception;

/**
 * Se lanza al cambiar la contraseña estando autenticado (distinto del flujo
 * de recuperación por email) cuando la "contraseña actual" enviada no
 * coincide con la que tiene guardada el usuario.
 */
public class PasswordActualIncorrectaException extends RuntimeException {

    public PasswordActualIncorrectaException() {
        super("La contraseña actual no es correcta");
    }
}
