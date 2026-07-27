package com.almoby.ruralcuruzu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Motivo de la eliminación física de un comercio (documento, sección 12.1,
 * ícono de tacho en el listado). Es un borrado real e irreversible (a
 * diferencia de cambiar el estado a DADO_DE_BAJA), así que el motivo es
 * obligatorio y queda guardado en el registro de auditoría (ComercioEliminado).
 */
public record EliminarComercioRequest(

        @Schema(example = "Cerró el local definitivamente")
        @NotBlank(message = "El motivo es obligatorio para eliminar un comercio")
        String motivo

) {
}
