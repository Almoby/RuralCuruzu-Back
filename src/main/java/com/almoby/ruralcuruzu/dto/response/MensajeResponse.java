package com.almoby.ruralcuruzu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta genérica para endpoints que no tienen datos propios que devolver
 * (logout, forgot-password, reset-password) pero donde igual queremos
 * confirmarle al cliente, en el body, que la operación se completó.
 */
public record MensajeResponse(

        @Schema(description = "Mensaje legible para mostrarle al usuario")
        String mensaje

) {

    public static MensajeResponse of(String mensaje) {
        return new MensajeResponse(mensaje);
    }
}
