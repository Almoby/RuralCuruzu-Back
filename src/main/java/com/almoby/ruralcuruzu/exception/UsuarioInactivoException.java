package com.almoby.ruralcuruzu.exception;

/**
 * Se lanza cuando las credenciales son correctas pero la cuenta
 * está INACTIVO o SUSPENDIDO y no puede iniciar sesión.
 */
public class UsuarioInactivoException extends RuntimeException {

    public UsuarioInactivoException() {
        super("El usuario no se encuentra activo. Contactá al administrador");
    }
}
