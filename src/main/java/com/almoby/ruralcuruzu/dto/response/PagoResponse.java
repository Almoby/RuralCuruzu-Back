package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.almoby.ruralcuruzu.domain.Pago;
import com.almoby.ruralcuruzu.enums.EstadoPago;
import com.almoby.ruralcuruzu.enums.MedioPago;

/**
 * Un intento de pago (RN-17: Pago es su propia entidad). {@code comprobanteRuta}
 * es la misma ruta relativa que hay que mandar a
 * GET /api/socio/cuotas/pagos/{id}/comprobante para descargar el archivo,
 * igual que ya funciona para los adjuntos de una solicitud de socio.
 */
public record PagoResponse(

        String id,
        String cuotaId,
        String socioId,
        String socioNumeroSocio,
        String socioNombre,
        String periodo,
        BigDecimal importe,
        MedioPago medioPago,
        EstadoPago estado,
        Instant fechaPago,
        String comprobanteRuta,
        String observacion,
        boolean informadoPorSocio,
        String registradoPorAdminNombre,
        String motivoRechazo,
        Instant fechaCreacion

) {

    public static PagoResponse from(Pago pago) {
        if (pago == null) {
            return null;
        }
        return new PagoResponse(
                pago.getId(),
                pago.getCuotaId(),
                pago.getSocioId(),
                pago.getSocioNumeroSocio(),
                pago.getSocioNombre(),
                pago.getPeriodo(),
                pago.getImporte(),
                pago.getMedioPago(),
                pago.getEstado(),
                pago.getFechaPago(),
                pago.getComprobanteRuta(),
                pago.getObservacion(),
                pago.isInformadoPorSocio(),
                pago.getRegistradoPorAdminNombre(),
                pago.getMotivoRechazo(),
                pago.getFechaCreacion());
    }
}
