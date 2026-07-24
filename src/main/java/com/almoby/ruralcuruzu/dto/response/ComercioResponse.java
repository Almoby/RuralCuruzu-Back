package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;

import com.almoby.ruralcuruzu.domain.Comercio;
import com.almoby.ruralcuruzu.enums.EstadoComercio;

/**
 * Detalle completo de un comercio, para el admin.
 */
public record ComercioResponse(

        String id,
        String nombreComercial,
        String razonSocial,
        String cuit,
        String rubro,
        String telefono,
        String correoElectronico,
        String direccion,
        String logo,
        String descripcion,
        EstadoComercio estado,
        Instant fechaAlta,
        Instant fechaActualizacion

) {

    public static ComercioResponse from(Comercio comercio) {
        return new ComercioResponse(
                comercio.getId(),
                comercio.getNombreComercial(),
                comercio.getRazonSocial(),
                comercio.getCuit(),
                comercio.getRubro(),
                comercio.getTelefono(),
                comercio.getCorreoElectronico(),
                comercio.getDireccion(),
                comercio.getLogo(),
                comercio.getDescripcion(),
                comercio.getEstado(),
                comercio.getFechaAlta(),
                comercio.getFechaActualizacion());
    }
}
