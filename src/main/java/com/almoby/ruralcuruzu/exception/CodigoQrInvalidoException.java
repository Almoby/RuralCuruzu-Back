package com.almoby.ruralcuruzu.exception;

public class CodigoQrInvalidoException extends RuntimeException {

    public CodigoQrInvalidoException() {
        super("El código QR no corresponde a ningún socio");
    }
}
