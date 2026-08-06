package com.almoby.ruralcuruzu.exception;

public class TipoBeneficioNoEncontradoException extends RuntimeException {

    public TipoBeneficioNoEncontradoException(String id) {
        super("No existe un tipo de beneficio con id " + id);
    }
}
