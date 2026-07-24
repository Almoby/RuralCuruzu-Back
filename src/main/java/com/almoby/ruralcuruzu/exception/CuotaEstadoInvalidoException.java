package com.almoby.ruralcuruzu.exception;

/**
 * Se lanza cuando se intenta una acción sobre una cuota que no es válida
 * para su estado actual (ej. informar un pago sobre una cuota ya PAGADA,
 * o revisar una que no está EN_REVISION).
 */
public class CuotaEstadoInvalidoException extends RuntimeException {

    public CuotaEstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
