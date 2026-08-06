package com.almoby.ruralcuruzu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record TipoBeneficioActualizadoResponse(

        @Schema(example = "Tipo de beneficio actualizado con éxito")
        String mensaje,

        TipoBeneficioResponse tipoBeneficio

) {

    public static TipoBeneficioActualizadoResponse of(TipoBeneficioResponse tipoBeneficio) {
        return new TipoBeneficioActualizadoResponse("Tipo de beneficio actualizado con éxito", tipoBeneficio);
    }
}
