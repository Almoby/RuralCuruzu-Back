package com.almoby.ruralcuruzu.dto.response;

import com.almoby.ruralcuruzu.enums.EstadoTipoCuota;

import io.swagger.v3.oas.annotations.media.Schema;

public record CambiarEstadoTipoCuotaResponse(

        String id,

        EstadoTipoCuota estado,

        @Schema(example = "Tipo de cuota activado correctamente")
        String mensaje

) {

    public static CambiarEstadoTipoCuotaResponse of(String id, EstadoTipoCuota estado) {
        return new CambiarEstadoTipoCuotaResponse(id, estado, mensajePara(estado));
    }

    private static String mensajePara(EstadoTipoCuota estado) {
        return switch (estado) {
            case ACTIVO -> "Tipo de cuota activado correctamente";
            case INACTIVO -> "Tipo de cuota desactivado correctamente";
        };
    }
}
