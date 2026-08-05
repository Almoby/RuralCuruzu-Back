package com.almoby.ruralcuruzu.dto.response;

import com.almoby.ruralcuruzu.enums.EstadoSocio;

import io.swagger.v3.oas.annotations.media.Schema;

public record CambiarEstadoSocioResponse(

        String id,

        EstadoSocio estado,

        @Schema(example = "Socio dado de baja correctamente")
        String mensaje

) {

    public static CambiarEstadoSocioResponse of(String id, EstadoSocio estado) {
        return new CambiarEstadoSocioResponse(id, estado, mensajePara(estado));
    }

    private static String mensajePara(EstadoSocio estado) {
        return switch (estado) {
            case ACTIVO -> "Socio activado correctamente";
            case INACTIVO -> "Socio marcado como inactivo correctamente";
            case DADO_DE_BAJA -> "Socio dado de baja correctamente";
        };
    }
}
