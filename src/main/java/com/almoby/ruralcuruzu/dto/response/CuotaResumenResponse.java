package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.almoby.ruralcuruzu.domain.Cuota;
import com.almoby.ruralcuruzu.enums.EstadoCuota;
import com.almoby.ruralcuruzu.enums.MedioPago;

/**
 * Fila de listado, sin todos los detalles de CuotaResponse.
 * {@code medioPago} viene de {@code datosPago} y es null en cuotas que
 * todavía no tienen un pago registrado (PENDIENTE/VENCIDA sin informar).
 */
public record CuotaResumenResponse(

        String id,
        String socioNumeroSocio,
        String socioNombre,
        String periodo,
        BigDecimal importe,
        EstadoCuota estado,
        LocalDate fechaVencimiento,
        MedioPago medioPago

) {

    public static CuotaResumenResponse from(Cuota cuota) {
        return new CuotaResumenResponse(
                cuota.getId(),
                cuota.getSocioNumeroSocio(),
                cuota.getSocioNombre(),
                cuota.getPeriodo(),
                cuota.getImporte(),
                cuota.getEstado(),
                cuota.getFechaVencimiento(),
                cuota.getDatosPago() != null ? cuota.getDatosPago().getMedioPago() : null);
    }
}
