package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;
import java.util.List;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.domain.Comercio;
import com.almoby.ruralcuruzu.enums.EstadoBeneficio;
import com.almoby.ruralcuruzu.enums.EstadoComercio;

/**
 * Detalle completo de un comercio, para el admin. {@code promociones} se
 * arma en el service a partir de Beneficio + HistorialBeneficio (no vive en
 * el propio Comercio), por eso {@link #from} recibe esos datos ya resueltos
 * en vez de calcularlos acá.
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
        Instant fechaActualizacion,
        List<PromocionResumenResponse> promociones

) {

    public record PromocionResumenResponse(
            String id,
            String titulo,
            EstadoBeneficio estado,

            @io.swagger.v3.oas.annotations.media.Schema(description = "Cantidad de veces que se usó esta promoción en el mes en curso")
            long usosEsteMes
    ) {

        public static PromocionResumenResponse from(Beneficio beneficio, long usosEsteMes) {
            return new PromocionResumenResponse(beneficio.getId(), beneficio.getTitulo(), beneficio.getEstado(), usosEsteMes);
        }
    }

    public static ComercioResponse from(Comercio comercio, List<PromocionResumenResponse> promociones) {
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
                comercio.getFechaActualizacion(),
                promociones);
    }
}
