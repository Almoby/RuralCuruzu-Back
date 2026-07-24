package com.almoby.ruralcuruzu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta del alta de un comercio: un mensaje de éxito y, anidado bajo
 * "comercio", el objeto completo recién creado (mismos campos que
 * ComercioResponse).
 */
public record ComercioCreadoResponse(

        @Schema(example = "Comercio dado de alta con éxito")
        String mensaje,

        ComercioResponse comercio

) {

    public static ComercioCreadoResponse of(ComercioResponse comercio) {
        return new ComercioCreadoResponse("Comercio dado de alta con éxito", comercio);
    }
}
