package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.almoby.ruralcuruzu.domain.Cuota;
import com.almoby.ruralcuruzu.domain.Pago;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoCuota;

/**
 * {@code pagoVigente} es, de todos los {@link Pago} de esta cuota (puede
 * haber más de uno: RN-17), el más relevante para mostrar: el APROBADO si
 * existe, si no el que está EN_REVISION, y si no hay ninguno, {@code null}.
 * El historial completo (incluyendo intentos rechazados) se consulta aparte
 * vía CuotaService.listarPagosDeSocio.
 */
public record CuotaResponse(

        String id,
        String socioId,
        String socioNumeroSocio,
        String socioNombre,
        String tipoCuotaNombre,
        CategoriaSocio categoria,
        String periodo,
        BigDecimal importe,
        LocalDate fechaVencimiento,
        EstadoCuota estado,
        PagoResponse pagoVigente,
        String motivoRechazo,
        String motivoAnulacion,
        Instant fechaGeneracion,
        Instant fechaActualizacion

) {

    public static CuotaResponse from(Cuota cuota, Pago pagoVigente) {
        return new CuotaResponse(
                cuota.getId(),
                cuota.getSocioId(),
                cuota.getSocioNumeroSocio(),
                cuota.getSocioNombre(),
                cuota.getTipoCuotaNombre(),
                cuota.getCategoria(),
                cuota.getPeriodo(),
                cuota.getImporte(),
                cuota.getFechaVencimiento(),
                cuota.getEstado(),
                PagoResponse.from(pagoVigente),
                cuota.getMotivoRechazo(),
                cuota.getMotivoAnulacion(),
                cuota.getFechaGeneracion(),
                cuota.getFechaActualizacion());
    }
}
