package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.almoby.ruralcuruzu.domain.Cuota;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoCuota;

public record CuotaResponse(

        String id,
        String socioId,
        String socioNumeroSocio,
        String socioNombre,
        String tipoCuotaId,
        String tipoCuotaNombre,
        CategoriaSocio categoria,
        String periodo,
        BigDecimal importe,
        LocalDate fechaVencimiento,
        EstadoCuota estado,
        DatosPagoResponse datosPago,
        String motivoRechazo,
        String motivoAnulacion,
        Instant fechaGeneracion,
        Instant fechaActualizacion

) {

    public static CuotaResponse from(Cuota cuota) {
        return new CuotaResponse(
                cuota.getId(),
                cuota.getSocioId(),
                cuota.getSocioNumeroSocio(),
                cuota.getSocioNombre(),
                cuota.getTipoCuotaId(),
                cuota.getTipoCuotaNombre(),
                cuota.getCategoria(),
                cuota.getPeriodo(),
                cuota.getImporte(),
                cuota.getFechaVencimiento(),
                cuota.getEstado(),
                DatosPagoResponse.from(cuota.getDatosPago()),
                cuota.getMotivoRechazo(),
                cuota.getMotivoAnulacion(),
                cuota.getFechaGeneracion(),
                cuota.getFechaActualizacion());
    }
}
