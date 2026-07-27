package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lo que ve el solicitante en la página pública a la que llega desde el
 * link del correo, antes de responder (documento, sección 8.3: "solicitar
 * correcciones"/"solicitar documentación").
 */
public record ObservacionPendienteResponse(

        @Schema(example = "SOL-000123")
        String numeroSolicitud,

        String nombreSolicitante,

        @Schema(description = "Texto de la última observación dejada por un admin")
        String observacion,

        Instant fechaHora

) {
}
