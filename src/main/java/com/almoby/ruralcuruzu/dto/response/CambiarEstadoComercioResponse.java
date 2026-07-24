package com.almoby.ruralcuruzu.dto.response;

import com.almoby.ruralcuruzu.enums.EstadoComercio;

import io.swagger.v3.oas.annotations.media.Schema;

public record CambiarEstadoComercioResponse(

        String id,

        EstadoComercio estado,

        @Schema(example = "Comercio suspendido correctamente")
        String mensaje

) {

    public static CambiarEstadoComercioResponse of(String id, EstadoComercio estado) {
        return new CambiarEstadoComercioResponse(id, estado, mensajePara(estado));
    }

    private static String mensajePara(EstadoComercio estado) {
        return switch (estado) {
            case ACTIVO -> "Comercio activado correctamente";
            case INACTIVO -> "Comercio marcado como inactivo correctamente";
            case SUSPENDIDO -> "Comercio suspendido correctamente";
            case DADO_DE_BAJA -> "Comercio dado de baja correctamente";
        };
    }
}
