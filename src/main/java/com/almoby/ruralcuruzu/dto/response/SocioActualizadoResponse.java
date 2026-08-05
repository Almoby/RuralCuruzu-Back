package com.almoby.ruralcuruzu.dto.response;

public record SocioActualizadoResponse(

        String mensaje,
        SocioResponse socio

) {

    public static SocioActualizadoResponse of(SocioResponse socio) {
        return new SocioActualizadoResponse("Socio actualizado correctamente", socio);
    }
}
