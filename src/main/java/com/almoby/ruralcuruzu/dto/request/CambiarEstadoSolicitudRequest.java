package com.almoby.ruralcuruzu.dto.request;

import com.almoby.ruralcuruzu.enums.EstadoSolicitud;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de la petición para que un admin cambie el estado de una solicitud
 * (documento 5.5: cada cambio registra observación y, cuando corresponde, motivo).
 */
public record CambiarEstadoSolicitudRequest(

        @Schema(description = "Nuevo estado de la solicitud")
        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoSolicitud nuevoEstado,

        @Schema(description = "Comentario interno del admin sobre esta revisión", example = "Documentación verificada")
        String observacion,

        @Schema(description = "Obligatorio al rechazar o cancelar: por qué se tomó esa decisión",
                example = "El CUIT informado no coincide con la razón social")
        String motivo

) {
}
