package com.almoby.ruralcuruzu.exception;

/** Se lanza cuando un archivo adjunto no cumple el tipo o tamaño permitido. */
public class ArchivoInvalidoException extends RuntimeException {

    public ArchivoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
