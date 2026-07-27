package com.almoby.ruralcuruzu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de DELETE /api/admin/comercios/{id}: un mensaje de éxito y,
 * anidado bajo "comercio", el registro de auditoría recién creado (mismo
 * objeto que aparece en GET /eliminados).
 */
public record EliminarComercioResponse(

        @Schema(example = "Comercio eliminado correctamente")
        String mensaje,

        ComercioEliminadoResponse comercio

) {

    public static EliminarComercioResponse of(ComercioEliminadoResponse comercio) {
        return new EliminarComercioResponse("Comercio eliminado correctamente", comercio);
    }
}
