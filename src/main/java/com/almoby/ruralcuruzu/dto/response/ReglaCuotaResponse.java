package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.almoby.ruralcuruzu.domain.ReglaCuota;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;

public record ReglaCuotaResponse(

        String id,
        CategoriaSocio categoriaAplicable,
        String nombre,
        BigDecimal importe,
        int diaVencimiento,
        Instant fechaCreacion,
        Instant fechaActualizacion

) {

    public static ReglaCuotaResponse from(ReglaCuota regla) {
        return new ReglaCuotaResponse(
                regla.getId(),
                regla.getCategoriaAplicable(),
                regla.getNombre(),
                regla.getImporte(),
                regla.getDiaVencimiento(),
                regla.getFechaCreacion(),
                regla.getFechaActualizacion());
    }
}
