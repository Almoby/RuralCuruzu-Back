package com.almoby.ruralcuruzu.dto.response;

import com.almoby.ruralcuruzu.domain.Comercio;
import com.almoby.ruralcuruzu.enums.EstadoComercio;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Fila resumida para el listado del panel de administración (documento,
 * sección 12.1).
 */
public record ComercioResumenResponse(

        String id,
        String nombreComercial,
        String razonSocial,
        String cuit,
        String rubro,
        String telefono,
        String correoElectronico,
        String direccion,
        EstadoComercio estado,

        @Schema(description = "Se completa cuando exista el módulo de Beneficios/Promociones. Por ahora siempre 0.")
        long cantidadPromociones

) {

    public static ComercioResumenResponse from(Comercio comercio) {
        return new ComercioResumenResponse(
                comercio.getId(),
                comercio.getNombreComercial(),
                comercio.getRazonSocial(),
                comercio.getCuit(),
                comercio.getRubro(),
                comercio.getTelefono(),
                comercio.getCorreoElectronico(),
                comercio.getDireccion(),
                comercio.getEstado(),
                0L);
    }
}
