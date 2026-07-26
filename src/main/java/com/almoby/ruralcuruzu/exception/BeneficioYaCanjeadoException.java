package com.almoby.ruralcuruzu.exception;

/**
 * Cada beneficio se puede canjear una única vez por socio (decisión
 * explícita: "una sola vez para siempre", no por día).
 */
public class BeneficioYaCanjeadoException extends RuntimeException {

    public BeneficioYaCanjeadoException(String beneficioTitulo) {
        super("Este socio ya canjeó el beneficio " + beneficioTitulo + " anteriormente");
    }
}
