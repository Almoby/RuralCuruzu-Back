package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.almoby.ruralcuruzu.domain.HistorialBeneficio;

/** Confirmación que ve el comercio tras escanear y validar el QR de un socio. */
public record ValidarBeneficioResponse(

        String mensaje,
        String socioNombre,
        String beneficioTitulo,
        BigDecimal montoAhorro,
        Instant fechaUso

) {

    public static ValidarBeneficioResponse from(HistorialBeneficio historial) {
        return new ValidarBeneficioResponse(
                "Beneficio aplicado con éxito",
                historial.getSocioNombre(),
                historial.getBeneficioTitulo(),
                historial.getMontoAhorro(),
                historial.getFechaUso());
    }
}
