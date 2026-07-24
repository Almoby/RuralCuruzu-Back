package com.almoby.ruralcuruzu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record TipoCuotaCreadoResponse(

        @Schema(example = "Tipo de cuota creado con éxito")
        String mensaje,

        TipoCuotaResponse tipoCuota

) {

    public static TipoCuotaCreadoResponse of(TipoCuotaResponse tipoCuota) {
        return new TipoCuotaCreadoResponse("Tipo de cuota creado con éxito", tipoCuota);
    }
}
