package com.almoby.ruralcuruzu.exception;

public class ComercioNoEncontradoException extends RuntimeException {

    public ComercioNoEncontradoException(String id) {
        super("No existe ningún comercio con id " + id);
    }
}
