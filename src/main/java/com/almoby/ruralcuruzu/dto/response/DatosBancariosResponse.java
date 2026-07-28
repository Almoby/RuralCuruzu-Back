package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;

import com.almoby.ruralcuruzu.domain.DatosBancarios;

public record DatosBancariosResponse(

        String banco,
        String cbu,
        String alias,
        String titular,
        String cuit,
        Instant fechaActualizacion

) {

    public static DatosBancariosResponse from(DatosBancarios datosBancarios) {
        return new DatosBancariosResponse(
                datosBancarios.getBanco(),
                datosBancarios.getCbu(),
                datosBancarios.getAlias(),
                datosBancarios.getTitular(),
                datosBancarios.getCuit(),
                datosBancarios.getFechaActualizacion());
    }
}
