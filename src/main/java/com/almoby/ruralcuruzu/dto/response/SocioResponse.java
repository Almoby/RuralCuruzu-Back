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
 *
 * {@code estadoCuenta} viaja solo en el detalle individual ({@code GET
 * /api/admin/socios/{id}}, vía {@link #from(Socio, EstadoCuentaSocioResponse)}):
 * deuda total y el detalle de cuotas del socio, reutilizando
 * {@link com.almoby.ruralcuruzu.service.CuotaService#obtenerEstadoCuentaSocio(String)}
 * para que la pantalla de Gestión de Socios no tenga que combinar el detalle
 * del socio con una segunda llamada a Cuotas. En creación/edición (donde no
 * tiene sentido recalcularlo) queda en {@code null} vía {@link #from(Socio)}.
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
        Instant fechaActualizacion,
        EstadoCuentaSocioResponse estadoCuenta

) {

    public static SocioResponse from(Socio socio) {
        return from(socio, null);
    }

    public static SocioResponse from(Socio socio, EstadoCuentaSocioResponse estadoCuenta) {
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
                socio.getFechaActualizacion(),
                estadoCuenta);
    }
}
