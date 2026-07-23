package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;

import com.almoby.ruralcuruzu.domain.SolicitudSocio;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoSolicitud;
import com.almoby.ruralcuruzu.enums.TipoPersona;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Fila resumida para el listado del panel de administración: evita mandar
 * todos los datos personales completos solo para mostrar una tabla.
 */
public record SolicitudSocioResumenResponse(

        @Schema(example = "SOL-000123")
        String numeroSolicitud,

        @Schema(description = "Nombre y apellido (persona física) o razón social (persona jurídica)")
        String nombreParaMostrar,

        String email,

        CategoriaSocio categoriaSolicitada,

        TipoPersona tipoPersona,

        EstadoSolicitud estado,

        Instant fechaCreacion

) {
    public static SolicitudSocioResumenResponse from(SolicitudSocio solicitud) {
        return new SolicitudSocioResumenResponse(
                solicitud.getNumeroSolicitud(),
                solicitud.nombreParaMostrar(),
                solicitud.getEmail(),
                solicitud.getCategoriaSolicitada(),
                solicitud.getTipoPersona(),
                solicitud.getEstado(),
                solicitud.getFechaCreacion());
    }
}
