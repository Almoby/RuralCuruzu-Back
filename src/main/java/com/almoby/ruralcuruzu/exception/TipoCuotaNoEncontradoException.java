package com.almoby.ruralcuruzu.exception;

public class TipoCuotaNoEncontradoException extends RuntimeException {

    public TipoCuotaNoEncontradoException(String id) {
        super("No existe ningún tipo de cuota con id " + id);
    }
}
