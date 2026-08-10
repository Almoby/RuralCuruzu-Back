package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.domain.HistorialBeneficio;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;

/** Confirmación que ve el comercio tras escanear y validar el QR de un socio. */
public record ValidarBeneficioResponse(

        String mensaje,
        String socioNombre,
        String socioNumeroSocio,
        CategoriaSocio socioCategoria,
        String beneficioTitulo,
        String beneficioTipoNombre,
        String beneficioValor,
        BigDecimal montoAhorro,
        Instant fechaUso,
        /** Total de usos de ESTE socio en este beneficio, incluyendo el canje recién aplicado. */
        long usosDelSocio,
        /** Normalizado (nunca null): 1 = default legado, 0 = ilimitado. */
        int limiteUsosPorSocio,
        /** null = ilimitado (limiteUsosPorSocio == 0). */
        Integer usosRestantes

) {

    /** @deprecated usar {@link #from(HistorialBeneficio, Beneficio, long)}, que incluye el conteo de usos. */
    @Deprecated
    public static ValidarBeneficioResponse from(HistorialBeneficio historial) {
        return new ValidarBeneficioResponse(
                "Beneficio aplicado con éxito",
                historial.getSocioNombre(),
                historial.getSocioNumeroSocio(),
                historial.getSocioCategoria(),
                historial.getBeneficioTitulo(),
                historial.getTipoBeneficioNombre(),
                historial.getValor(),
                historial.getMontoAhorro(),
                historial.getFechaUso(),
                0L,
                0,
                null);
    }

    /**
     * @param usosDelSocio total de usos de ese socio en este beneficio, incluyendo el que se acaba de registrar.
     */
    public static ValidarBeneficioResponse from(HistorialBeneficio historial, Beneficio beneficio, long usosDelSocio) {
        return new ValidarBeneficioResponse(
                "Beneficio aplicado con éxito",
                historial.getSocioNombre(),
                historial.getSocioNumeroSocio(),
                historial.getSocioCategoria(),
                historial.getBeneficioTitulo(),
                historial.getTipoBeneficioNombre(),
                historial.getValor(),
                historial.getMontoAhorro(),
                historial.getFechaUso(),
                usosDelSocio,
                beneficio.limiteUsosPorSocioEfectivo(),
                beneficio.usosRestantes(usosDelSocio));
    }
}
