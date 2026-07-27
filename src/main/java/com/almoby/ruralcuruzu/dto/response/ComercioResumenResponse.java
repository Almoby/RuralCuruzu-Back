package com.almoby.ruralcuruzu.dto.response;

import com.almoby.ruralcuruzu.domain.Comercio;
import com.almoby.ruralcuruzu.enums.EstadoComercio;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Fila resumida para el listado del panel de administración (documento,
 * sección 12.1). {@code cantidadPromociones} y {@code consumosTotales} se
 * calculan en el service (a partir de Beneficio y HistorialBeneficio, no
 * viven en el propio Comercio) y se pasan ya resueltos a {@link #from}.
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

        long cantidadPromociones,

        @Schema(description = "Cantidad total de veces que se usó alguna promoción de este comercio (historial completo).")
        long consumosTotales

) {

    public static ComercioResumenResponse from(Comercio comercio, long cantidadPromociones, long consumosTotales) {
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
                cantidadPromociones,
                consumosTotales);
    }
}
