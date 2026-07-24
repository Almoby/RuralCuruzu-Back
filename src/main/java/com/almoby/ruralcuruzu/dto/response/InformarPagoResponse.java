package com.almoby.ruralcuruzu.dto.response;

public record InformarPagoResponse(

        String mensaje,
        CuotaResponse cuota

) {

    public static InformarPagoResponse of(CuotaResponse cuota) {
        return new InformarPagoResponse("Pago informado con éxito, queda a la espera de revisión", cuota);
    }
}
