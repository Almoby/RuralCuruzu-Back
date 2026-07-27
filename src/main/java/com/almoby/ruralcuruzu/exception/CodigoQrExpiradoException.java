package com.almoby.ruralcuruzu.exception;

/**
 * Se lanza cuando el token del QR de un socio (documento, sección 15) es
 * sintácticamente válido pero ya venció su corta ventana de vigencia. Es el
 * caso esperado si el comercio tarda en escanear: hay que pedirle al socio
 * que vuelva a abrir "Mi QR" para generar uno nuevo, no que su membresía
 * tenga un problema (eso lo cubre {@link QrNoValidoException}).
 */
public class CodigoQrExpiradoException extends RuntimeException {

    public CodigoQrExpiradoException() {
        super("El código QR expiró. Pedile al socio que actualice su QR e intentá de nuevo");
    }
}
