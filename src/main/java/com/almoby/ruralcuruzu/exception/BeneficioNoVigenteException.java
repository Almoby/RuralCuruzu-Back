package com.almoby.ruralcuruzu.exception;

public class BeneficioNoVigenteException extends RuntimeException {

    public BeneficioNoVigenteException(String beneficioId) {
        super("El beneficio " + beneficioId + " no está vigente (pausado o vencido)");
    }
}
