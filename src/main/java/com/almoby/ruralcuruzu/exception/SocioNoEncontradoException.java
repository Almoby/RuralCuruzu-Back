package com.almoby.ruralcuruzu.exception;

public class SocioNoEncontradoException extends RuntimeException {

    public SocioNoEncontradoException(String id) {
        super("No existe ningún socio con id " + id);
    }
}
