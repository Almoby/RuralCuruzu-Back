package com.almoby.ruralcuruzu.exception;

/**
 * El QR del socio no está ACTIVO en este momento (documento, sección 15.2):
 * el comercio no puede aplicarle un beneficio. El mensaje ya viene armado
 * por EstadoQrService, explicando el motivo puntual (deuda, suspensión, etc.).
 */
public class QrNoValidoException extends RuntimeException {

    public QrNoValidoException(String mensaje) {
        super(mensaje);
    }
}
