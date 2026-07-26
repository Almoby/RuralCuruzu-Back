package com.almoby.ruralcuruzu.exception;

public class BeneficioNoEncontradoException extends RuntimeException {

    public BeneficioNoEncontradoException(String id) {
        super("No existe ningún beneficio con id " + id);
    }
}
