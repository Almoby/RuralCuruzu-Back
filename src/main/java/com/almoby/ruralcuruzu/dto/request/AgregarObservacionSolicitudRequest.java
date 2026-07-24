package com.almoby.ruralcuruzu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo para agregar una observación a una solicitud sin cambiar su estado
 * (documento, sección 8.3: "agregar observaciones", "solicitar correcciones"
 * y "solicitar documentación" son distintos casos de uso de lo mismo: una
 * nota visible en el historial dirigida al solicitante).
 */
public record AgregarObservacionSolicitudRequest(

        @Schema(example = "Falta adjuntar el comprobante de domicilio")
        @NotBlank(message = "La observación es obligatoria")
        String observacion

) {
}
