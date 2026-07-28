package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.almoby.ruralcuruzu.domain.HistorialBeneficio;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.TipoBeneficio;

/** Confirmación que ve el comercio tras escanear y validar el QR de un socio. */
public record ValidarBeneficioResponse(

        String mensaje,
        String socioNombre,
        String socioNumeroSocio,
        CategoriaSocio socioCategoria,
        String beneficioTitulo,
        TipoBeneficio beneficioTipo,
        String beneficioValor,
        BigDecimal montoAhorro,
        Instant fechaUso

) {

    public static ValidarBeneficioResponse from(HistorialBeneficio historial) {
        return new ValidarBeneficioResponse(
                "Beneficio aplicado con éxito",
                historial.getSocioNombre(),
                historial.getSocioNumeroSocio(),
                historial.getSocioCategoria(),
                historial.getBeneficioTitulo(),
                historial.getTipo(),
                historial.getValor(),
                historial.getMontoAhorro(),
                historial.getFechaUso());
    }
}
