package com.almoby.ruralcuruzu.exception;

public class TipoBeneficioCodigoDuplicadoException extends RuntimeException {

    public TipoBeneficioCodigoDuplicadoException(String codigo) {
        super("Ya existe un tipo de beneficio con código " + codigo);
    }
}
