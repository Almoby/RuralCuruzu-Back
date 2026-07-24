package com.almoby.ruralcuruzu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de la creación de una solicitud de socio: un mensaje de éxito y,
 * anidado bajo "solicitud", el objeto completo recién creado (mismos campos
 * que SolicitudSocioResponse).
 */
public record SolicitudSocioCreadaResponse(

        @Schema(example = "Solicitud de socio enviada con éxito")
        String mensaje,

        SolicitudSocioResponse solicitud

) {

    public static SolicitudSocioCreadaResponse of(SolicitudSocioResponse solicitud) {
        return new SolicitudSocioCreadaResponse("Solicitud de socio enviada con éxito", solicitud);
    }
}
