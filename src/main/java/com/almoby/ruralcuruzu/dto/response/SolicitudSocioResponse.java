package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;
import java.util.List;

import com.almoby.ruralcuruzu.domain.DatosPersonaFisica;
import com.almoby.ruralcuruzu.domain.DatosPersonaJuridica;
import com.almoby.ruralcuruzu.domain.SolicitudSocio;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoSolicitud;
import com.almoby.ruralcuruzu.enums.TipoPersona;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Detalle completo de una solicitud (para el solicitante tras enviarla,
 * y para el admin al revisarla).
 */
public record SolicitudSocioResponse(

        @Schema(example = "SOL-000123")
        String numeroSolicitud,

        EstadoSolicitud estado,

        CategoriaSocio categoriaSolicitada,

        TipoPersona tipoPersona,

        DatosPersonaFisica datosPersonaFisica,

        DatosPersonaJuridica datosPersonaJuridica,

        Instant fechaCreacion,

        Instant fechaActualizacion,

        List<CambioEstadoResponse> historial

) {

    public record CambioEstadoResponse(
            EstadoSolicitud estadoAnterior,
            EstadoSolicitud estadoNuevo,
            Instant fechaHora,
            String adminResponsableNombre,
            String observacion,
            String motivo
    ) {
    }

    public static SolicitudSocioResponse from(SolicitudSocio solicitud) {
        List<CambioEstadoResponse> historial = solicitud.getHistorial().stream()
                .map(cambio -> new CambioEstadoResponse(
                        cambio.getEstadoAnterior(),
                        cambio.getEstadoNuevo(),
                        cambio.getFechaHora(),
                        cambio.getAdminResponsableNombre(),
                        cambio.getObservacion(),
                        cambio.getMotivo()))
                .toList();

        return new SolicitudSocioResponse(
                solicitud.getNumeroSolicitud(),
                solicitud.getEstado(),
                solicitud.getCategoriaSolicitada(),
                solicitud.getTipoPersona(),
                solicitud.getDatosPersonaFisica(),
                solicitud.getDatosPersonaJuridica(),
                solicitud.getFechaCreacion(),
                solicitud.getFechaActualizacion(),
                historial);
    }
}
