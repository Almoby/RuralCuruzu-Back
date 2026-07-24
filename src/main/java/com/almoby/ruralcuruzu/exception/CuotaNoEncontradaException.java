package com.almoby.ruralcuruzu.exception;

public class CuotaNoEncontradaException extends RuntimeException {

    public CuotaNoEncontradaException(String id) {
        super("No existe ninguna cuota con id " + id);
    }

    public static CuotaNoEncontradaException paraSocioYPeriodo(String socioId, String periodo) {
        return new CuotaNoEncontradaException(
                "No existe una cuota generada para el socio " + socioId + " en el período " + periodo, true);
    }

    private CuotaNoEncontradaException(String mensaje, boolean mensajeLiteral) {
        super(mensaje);
    }
}
