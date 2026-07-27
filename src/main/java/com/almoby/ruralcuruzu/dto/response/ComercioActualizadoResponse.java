package com.almoby.ruralcuruzu.dto.response;

public record ComercioActualizadoResponse(

        String mensaje,
        ComercioResponse comercio

) {

    public static ComercioActualizadoResponse of(ComercioResponse comercio) {
        return new ComercioActualizadoResponse("Comercio actualizado correctamente", comercio);
    }
}
