package com.almoby.ruralcuruzu.exception;

import com.almoby.ruralcuruzu.enums.EstadoSolicitud;

public class TransicionEstadoInvalidaException extends RuntimeException {

    public TransicionEstadoInvalidaException(EstadoSolicitud actual, EstadoSolicitud solicitado) {
        super("No se puede pasar del estado " + actual + " a " + solicitado);
    }

    public TransicionEstadoInvalidaException(String mensaje) {
        super(mensaje);
    }
}
