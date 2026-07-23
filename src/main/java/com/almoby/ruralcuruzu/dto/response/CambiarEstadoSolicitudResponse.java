package com.almoby.ruralcuruzu.dto.response;

import com.almoby.ruralcuruzu.enums.EstadoSolicitud;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta acotada para el cambio de estado de una solicitud: no hace falta
 * devolver el detalle completo (datos personales + historial entero) solo
 * para confirmar que el cambio se aplicó. Para ver el detalle completo está
 * el endpoint GET /api/admin/solicitudes-socio/{numeroSolicitud}.
 */
public record CambiarEstadoSolicitudResponse(

        @Schema(example = "SOL-000123")
        String numeroSolicitud,

        EstadoSolicitud estado,

        @Schema(description = "Mensaje legible para mostrarle al admin", example = "Solicitud aprobada correctamente")
        String mensaje

) {

    public static CambiarEstadoSolicitudResponse of(String numeroSolicitud, EstadoSolicitud estado) {
        return new CambiarEstadoSolicitudResponse(numeroSolicitud, estado, mensajePara(estado));
    }

    private static String mensajePara(EstadoSolicitud estado) {
        return switch (estado) {
            case PENDIENTE -> "Solicitud pendiente de revisión";
            case EN_REVISION -> "Solicitud puesta en revisión correctamente";
            case APROBADA -> "Solicitud aprobada correctamente";
            case RECHAZADA -> "Solicitud rechazada correctamente";
            case CANCELADA -> "Solicitud cancelada correctamente";
        };
    }
}
