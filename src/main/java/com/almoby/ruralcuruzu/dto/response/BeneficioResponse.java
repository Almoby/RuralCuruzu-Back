package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;
import java.time.LocalDate;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.enums.EstadoBeneficio;
import com.almoby.ruralcuruzu.enums.TipoBeneficio;

/** Detalle completo de un beneficio, para el comercio que lo administra. */
public record BeneficioResponse(

        String id,
        String comercioId,
        String comercioNombre,
        String titulo,
        String descripcion,
        TipoBeneficio tipo,
        String valor,
        LocalDate fechaInicioVigencia,
        LocalDate fechaFinVigencia,
        EstadoBeneficio estado,
        boolean vigenteHoy,
        Instant fechaCreacion,
        Instant fechaActualizacion

) {

    public static BeneficioResponse from(Beneficio beneficio) {
        return new BeneficioResponse(
                beneficio.getId(),
                beneficio.getComercioId(),
                beneficio.getComercioNombre(),
                beneficio.getTitulo(),
                beneficio.getDescripcion(),
                beneficio.getTipo(),
                beneficio.getValor(),
                beneficio.getFechaInicioVigencia(),
                beneficio.getFechaFinVigencia(),
                beneficio.getEstado(),
                beneficio.estaVigenteHoy(),
                beneficio.getFechaCreacion(),
                beneficio.getFechaActualizacion());
    }
}
