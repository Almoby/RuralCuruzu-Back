package com.almoby.ruralcuruzu.dto.response;

import java.util.List;

import com.almoby.ruralcuruzu.domain.Comercio;

/** Fila de la pestaña "Comercios" del socio (Figma, sección 14). */
public record ComercioConBeneficiosResponse(

        String id,
        String nombreComercial,
        String rubro,
        String direccion,
        String telefono,
        String logo,
        List<BeneficioResumenResponse> beneficios

) {

    public static ComercioConBeneficiosResponse of(Comercio comercio, List<BeneficioResumenResponse> beneficios) {
        return new ComercioConBeneficiosResponse(
                comercio.getId(),
                comercio.getNombreComercial(),
                comercio.getRubro(),
                comercio.getDireccion(),
                comercio.getTelefono(),
                comercio.getLogo(),
                beneficios);
    }
}
