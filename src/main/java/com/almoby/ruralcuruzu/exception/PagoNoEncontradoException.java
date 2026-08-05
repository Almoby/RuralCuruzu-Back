package com.almoby.ruralcuruzu.exception;

public class PagoNoEncontradoException extends RuntimeException {

    public PagoNoEncontradoException(String id) {
        super("No existe ningún pago con id " + id);
    }
}
