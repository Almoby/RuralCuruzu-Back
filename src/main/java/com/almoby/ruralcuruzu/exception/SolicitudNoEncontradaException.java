package com.almoby.ruralcuruzu.exception;

public class SolicitudNoEncontradaException extends RuntimeException {

    public SolicitudNoEncontradaException(String numeroSolicitud) {
        super("No existe ninguna solicitud con número " + numeroSolicitud);
    }
}
