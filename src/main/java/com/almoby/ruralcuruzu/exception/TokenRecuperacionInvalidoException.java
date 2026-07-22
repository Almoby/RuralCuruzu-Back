package com.almoby.ruralcuruzu.exception;

/**
 * Se lanza cuando el token de recuperación de contraseña no existe,
 * ya fue usado, o no corresponde a ninguno emitido por el sistema.
 * Mensaje genérico: no distinguimos "no existe" de "ya usado" para no
 * darle pistas a quien intente adivinar o reusar tokens.
 */
public class TokenRecuperacionInvalidoException extends RuntimeException {

    public TokenRecuperacionInvalidoException() {
        super("El enlace de recuperación no es válido o ya fue utilizado");
    }
}
