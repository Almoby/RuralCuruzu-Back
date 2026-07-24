package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.almoby.ruralcuruzu.domain.DatosPago;
import com.almoby.ruralcuruzu.enums.MedioPago;

public record DatosPagoResponse(

        Instant fechaPago,
        BigDecimal importe,
        MedioPago medioPago,
        String comprobante,
        String observacion,
        boolean informadoPorSocio,
        String registradoPorAdminNombre

) {

    public static DatosPagoResponse from(DatosPago datosPago) {
        if (datosPago == null) {
            return null;
        }
        return new DatosPagoResponse(
                datosPago.getFechaPago(),
                datosPago.getImporte(),
                datosPago.getMedioPago(),
                datosPago.getComprobante(),
                datosPago.getObservacion(),
                datosPago.isInformadoPorSocio(),
                datosPago.getRegistradoPorAdminNombre());
    }
}
