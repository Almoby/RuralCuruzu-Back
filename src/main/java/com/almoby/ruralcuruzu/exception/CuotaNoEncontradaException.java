package com.almoby.ruralcuruzu.exception;

public class CuotaNoEncontradaException extends RuntimeException {

    public CuotaNoEncontradaException(String id) {
        super("No existe ninguna cuota con id " + id);
    }
}
