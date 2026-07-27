package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;

import com.almoby.ruralcuruzu.domain.ComercioEliminado;
import com.almoby.ruralcuruzu.enums.EstadoComercio;

/** Fila del historial de comercios eliminados (auditoría, ver ComercioEliminado). */
public record ComercioEliminadoResponse(

        String id,
        String comercioIdOriginal,
        String nombreComercial,
        String razonSocial,
        String cuit,
        String rubro,
        EstadoComercio estadoAlEliminar,
        String motivo,
        String adminResponsableBajaNombre,
        Instant fechaBaja

) {

    public static ComercioEliminadoResponse from(ComercioEliminado eliminado) {
        return new ComercioEliminadoResponse(
                eliminado.getId(),
                eliminado.getComercioIdOriginal(),
                eliminado.getNombreComercial(),
                eliminado.getRazonSocial(),
                eliminado.getCuit(),
                eliminado.getRubro(),
                eliminado.getEstadoAlEliminar(),
                eliminado.getMotivo(),
                eliminado.getAdminResponsableBajaNombre(),
                eliminado.getFechaBaja());
    }
}
