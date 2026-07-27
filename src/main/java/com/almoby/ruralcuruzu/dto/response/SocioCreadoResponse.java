package com.almoby.ruralcuruzu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta del alta manual de un socio (documento, sección 9.5): un mensaje
 * de éxito y, anidado bajo "socio", el objeto completo recién creado (mismos
 * campos que SocioResponse).
 */
public record SocioCreadoResponse(

        @Schema(example = "Socio dado de alta con éxito")
        String mensaje,

        SocioResponse socio

) {

    public static SocioCreadoResponse of(SocioResponse socio) {
        return new SocioCreadoResponse("Socio dado de alta con éxito", socio);
    }
}
