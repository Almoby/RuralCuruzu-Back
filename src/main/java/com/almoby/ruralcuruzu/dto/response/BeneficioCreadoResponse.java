package com.almoby.ruralcuruzu.dto.response;

public record BeneficioCreadoResponse(

        String mensaje,
        BeneficioResponse beneficio

) {

    public static BeneficioCreadoResponse of(BeneficioResponse beneficio) {
        return new BeneficioCreadoResponse("Beneficio creado con éxito", beneficio);
    }
}
