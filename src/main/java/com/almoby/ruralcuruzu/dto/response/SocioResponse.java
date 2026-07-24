package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;

import com.almoby.ruralcuruzu.domain.DatosPersonaFisica;
import com.almoby.ruralcuruzu.domain.DatosPersonaJuridica;
import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.TipoPersona;

/**
 * Detalle completo de un socio, para el admin.
 */
public record SocioResponse(

        String id,
        String numeroSocio,
        String nombre,
        CategoriaSocio categoria,
        TipoPersona tipoPersona,
        DatosPersonaFisica datosPersonaFisica,
        DatosPersonaJuridica datosPersonaJuridica,
        EstadoSocio estado,
        String numeroSolicitudOrigen,
        Instant fechaAlta,
        Instant fechaActualizacion

) {

    public static SocioResponse from(Socio socio) {
        return new SocioResponse(
                socio.getId(),
                socio.getNumeroSocio(),
                socio.nombreParaMostrar(),
                socio.getCategoria(),
                socio.getTipoPersona(),
                socio.getDatosPersonaFisica(),
                socio.getDatosPersonaJuridica(),
                socio.getEstado(),
                socio.getNumeroSolicitudOrigen(),
                socio.getFechaAlta(),
                socio.getFechaActualizacion());
    }
}
