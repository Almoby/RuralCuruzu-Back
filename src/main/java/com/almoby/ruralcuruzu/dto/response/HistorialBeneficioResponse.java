package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.almoby.ruralcuruzu.domain.HistorialBeneficio;
import com.almoby.ruralcuruzu.enums.EstadoUsoBeneficio;
import com.almoby.ruralcuruzu.enums.TipoBeneficio;

/** Fila del historial de beneficios usados por un socio (documento, secciones 14.4 y 19.3). */
public record HistorialBeneficioResponse(

        String id,
        String comercioNombre,
        String beneficioTitulo,
        TipoBeneficio tipo,
        String valor,
        BigDecimal montoAhorro,
        EstadoUsoBeneficio estado,
        Instant fechaUso

) {

    public static HistorialBeneficioResponse from(HistorialBeneficio historial) {
        return new HistorialBeneficioResponse(
                historial.getId(),
                historial.getComercioNombre(),
                historial.getBeneficioTitulo(),
                historial.getTipo(),
                historial.getValor(),
                historial.getMontoAhorro(),
                historial.getEstado(),
                historial.getFechaUso());
    }
}
