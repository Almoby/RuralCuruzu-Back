package com.almoby.ruralcuruzu.dto.response;

import java.util.List;

public record RegistrarPagoResponse(

        String mensaje,
        List<CuotaResponse> cuotas

) {

    public static RegistrarPagoResponse of(List<CuotaResponse> cuotas) {
        return new RegistrarPagoResponse("Pago registrado con éxito", cuotas);
    }
}
