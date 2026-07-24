package com.almoby.ruralcuruzu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta acotada al agregar una observación a una solicitud (no cambia
 * el estado, así que no hace falta devolver el detalle completo).
 */
public record ObservacionAgregadaResponse(

        @Schema(example = "SOL-000123")
        String numeroSolicitud,

        @Schema(example = "Observación agregada correctamente")
        String mensaje

) {

    public static ObservacionAgregadaResponse of(String numeroSolicitud) {
        return new ObservacionAgregadaResponse(numeroSolicitud, "Observación agregada correctamente");
    }
}
