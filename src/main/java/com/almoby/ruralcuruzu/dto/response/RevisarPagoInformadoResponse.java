package com.almoby.ruralcuruzu.dto.response;

public record RevisarPagoInformadoResponse(

        String mensaje,
        CuotaResponse cuota

) {

    public static RevisarPagoInformadoResponse of(CuotaResponse cuota, boolean aprobado) {
        String mensaje = aprobado ? "Pago aprobado con éxito" : "Pago rechazado con éxito";
        return new RevisarPagoInformadoResponse(mensaje, cuota);
    }
}
