package com.almoby.ruralcuruzu.exception;

/**
 * El socio ya alcanzó el límite de usos que el comercio configuró para ese
 * beneficio (documento: por defecto era fijo en 1 "para siempre", ahora es
 * configurable por beneficio — ver Beneficio.limiteUsosPorSocio).
 */
public class BeneficioYaCanjeadoException extends RuntimeException {

    public BeneficioYaCanjeadoException(String beneficioTitulo, int limiteUsosPorSocio) {
        super(mensaje(beneficioTitulo, limiteUsosPorSocio));
    }

    private static String mensaje(String beneficioTitulo, int limiteUsosPorSocio) {
        if (limiteUsosPorSocio == 1) {
            // Mismo mensaje de siempre para el caso más común (límite 1), para no
            // sorprender a nadie que ya dependa de este texto.
            return "Este socio ya canjeó el beneficio " + beneficioTitulo + " anteriormente";
        }
        return "Este socio ya alcanzó el límite de " + limiteUsosPorSocio + " usos del beneficio " + beneficioTitulo;
    }
}
