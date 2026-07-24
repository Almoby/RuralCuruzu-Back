package com.almoby.ruralcuruzu.dto.response;

public record ReglaCuotaActualizadaResponse(

        String mensaje,
        ReglaCuotaResponse regla

) {

    public static ReglaCuotaActualizadaResponse of(ReglaCuotaResponse regla, boolean esNueva) {
        String mensaje = esNueva ? "Regla de cuota creada con éxito" : "Regla de cuota actualizada con éxito";
        return new ReglaCuotaActualizadaResponse(mensaje, regla);
    }
}
