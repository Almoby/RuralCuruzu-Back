package com.almoby.ruralcuruzu.dto.response;

public record DatosBancariosActualizadosResponse(

        String mensaje,
        DatosBancariosResponse datosBancarios

) {

    public static DatosBancariosActualizadosResponse of(DatosBancariosResponse datosBancarios, boolean esNuevo) {
        String mensaje = esNuevo ? "Datos bancarios creados con éxito" : "Datos bancarios actualizados con éxito";
        return new DatosBancariosActualizadosResponse(mensaje, datosBancarios);
    }
}
