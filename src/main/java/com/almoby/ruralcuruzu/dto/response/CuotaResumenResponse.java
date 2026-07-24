package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.almoby.ruralcuruzu.domain.Cuota;
import com.almoby.ruralcuruzu.enums.EstadoCuota;

/** Fila de listado, sin todos los detalles de CuotaResponse. */
public record CuotaResumenResponse(

        String id,
        String socioNumeroSocio,
        String socioNombre,
        String periodo,
        BigDecimal importe,
        EstadoCuota estado,
        LocalDate fechaVencimiento

) {

    public static CuotaResumenResponse from(Cuota cuota) {
        return new CuotaResumenResponse(
                cuota.getId(),
                cuota.getSocioNumeroSocio(),
                cuota.getSocioNombre(),
                cuota.getPeriodo(),
                cuota.getImporte(),
                cuota.getEstado(),
                cuota.getFechaVencimiento());
    }
}
