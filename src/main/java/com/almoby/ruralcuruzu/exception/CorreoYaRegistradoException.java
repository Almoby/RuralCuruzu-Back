package com.almoby.ruralcuruzu.exception;

/**
 * El correo ya pertenece a un Usuario existente o a otro Comercio
 * (documento, sección 12.2).
 */
public class CorreoYaRegistradoException extends RuntimeException {

    public CorreoYaRegistradoException() {
        super("Ya existe una cuenta registrada con ese correo electrónico");
    }
}
