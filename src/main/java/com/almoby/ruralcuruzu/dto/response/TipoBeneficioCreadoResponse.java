package com.almoby.ruralcuruzu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record TipoBeneficioCreadoResponse(

        @Schema(example = "Tipo de beneficio creado con éxito")
        String mensaje,

        TipoBeneficioResponse tipoBeneficio

) {

    public static TipoBeneficioCreadoResponse of(TipoBeneficioResponse tipoBeneficio) {
        return new TipoBeneficioCreadoResponse("Tipo de beneficio creado con éxito", tipoBeneficio);
    }
}
