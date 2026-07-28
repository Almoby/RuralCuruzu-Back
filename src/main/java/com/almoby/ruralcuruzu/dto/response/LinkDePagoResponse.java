package com.almoby.ruralcuruzu.dto.response;

public record LinkDePagoResponse(

        String mensaje,
        String pagoId,
        String linkDePago

) {

    public static LinkDePagoResponse of(String pagoId, String initPoint) {
        return new LinkDePagoResponse("Link de pago generado con éxito", pagoId, initPoint);
    }
}
