package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;

import com.almoby.ruralcuruzu.domain.TipoBeneficioCatalogo;

public record TipoBeneficioResponse(

        String id,
        String codigo,
        String nombre,
        boolean activo,
        Instant fechaCreacion,
        Instant fechaActualizacion

) {

    public static TipoBeneficioResponse from(TipoBeneficioCatalogo tipo) {
        return new TipoBeneficioResponse(
                tipo.getId(),
                tipo.getCodigo(),
                tipo.getNombre(),
                tipo.isActivo(),
                tipo.getFechaCreacion(),
                tipo.getFechaActualizacion());
    }
}
