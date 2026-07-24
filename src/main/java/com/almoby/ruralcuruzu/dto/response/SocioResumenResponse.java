package com.almoby.ruralcuruzu.dto.response;

import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.TipoPersona;

/**
 * Fila resumida para listados y selects (ej. el select de socio del modal
 * "Registrar pago" de Cuotas).
 */
public record SocioResumenResponse(

        String id,
        String numeroSocio,
        String nombre,
        CategoriaSocio categoria,
        TipoPersona tipoPersona,
        EstadoSocio estado,
        String correoElectronico

) {

    public static SocioResumenResponse from(Socio socio) {
        return new SocioResumenResponse(
                socio.getId(),
                socio.getNumeroSocio(),
                socio.nombreParaMostrar(),
                socio.getCategoria(),
                socio.getTipoPersona(),
                socio.getEstado(),
                socio.obtenerEmail());
    }
}
