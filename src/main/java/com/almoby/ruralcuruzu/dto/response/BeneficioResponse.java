package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;
import java.time.LocalDate;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.enums.EstadoBeneficio;

/**
 * Detalle completo de un beneficio, para el comercio que lo administra.
 * {@code estado} es siempre el efectivo (ver {@link Beneficio#estadoEfectivo()}):
 * ya da INACTIVO apenas se cumple fechaFinVigencia, sin esperar al job diario
 * ni depender de un campo aparte — el front no necesita mirar nada más que
 * este campo para saber si la promoción está activa.
 */
public record BeneficioResponse(

        String id,
        String comercioId,
        String comercioNombre,
        String titulo,
        String descripcion,
        String tipoBeneficioId,
        String tipoBeneficioNombre,
        String valor,
        LocalDate fechaInicioVigencia,
        LocalDate fechaFinVigencia,
        EstadoBeneficio estado,
        long usosEsteMes,
        Instant fechaCreacion,
        Instant fechaActualizacion

) {

    /** Beneficio recién creado: todavía no tiene ningún uso registrado. */
    public static BeneficioResponse from(Beneficio beneficio) {
        return from(beneficio, 0L);
    }

    public static BeneficioResponse from(Beneficio beneficio, long usosEsteMes) {
        return new BeneficioResponse(
                beneficio.getId(),
                beneficio.getComercioId(),
                beneficio.getComercioNombre(),
                beneficio.getTitulo(),
                beneficio.getDescripcion(),
                beneficio.getTipoBeneficioId(),
                beneficio.getTipoBeneficioNombre(),
                beneficio.getValor(),
                beneficio.getFechaInicioVigencia(),
                beneficio.getFechaFinVigencia(),
                beneficio.estadoEfectivo(),
                usosEsteMes,
                beneficio.getFechaCreacion(),
                beneficio.getFechaActualizacion());
    }
}
