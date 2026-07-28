package com.almoby.ruralcuruzu.exception;

import com.almoby.ruralcuruzu.enums.EstadoQr;

import lombok.Getter;

/**
 * El QR del socio no está ACTIVO en este momento (documento, sección 15.2):
 * el comercio no puede aplicarle un beneficio. El mensaje ya viene armado
 * por EstadoQrService, explicando el motivo puntual (deuda, suspensión, etc.).
 * {@code motivo} expone ese mismo caso como código estable (el {@link EstadoQr}
 * calculado), para que el comercio arme su propia pantalla de rechazo sin
 * tener que parsear el mensaje en texto libre.
 */
@Getter
public class QrNoValidoException extends RuntimeException {

    private final EstadoQr motivo;

    public QrNoValidoException(EstadoQr motivo, String mensaje) {
        super(mensaje);
        this.motivo = motivo;
    }
}
