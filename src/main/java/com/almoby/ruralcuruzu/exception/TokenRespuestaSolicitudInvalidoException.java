package com.almoby.ruralcuruzu.exception;

/**
 * Se lanza cuando el token para responder una observación de solicitud no
 * existe, ya fue usado, o no corresponde a ninguno emitido por el sistema.
 * Mensaje genérico: no distinguimos "no existe" de "ya usado" para no darle
 * pistas a quien intente adivinar o reusar tokens.
 */
public class TokenRespuestaSolicitudInvalidoException extends RuntimeException {

    public TokenRespuestaSolicitudInvalidoException() {
        super("El enlace no es válido o ya fue utilizado");
    }
}
