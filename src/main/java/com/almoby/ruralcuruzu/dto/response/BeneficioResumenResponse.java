package com.almoby.ruralcuruzu.dto.response;

import java.time.LocalDate;

import com.almoby.ruralcuruzu.domain.Beneficio;

/** Fila para el listado de "Beneficios y Comercios" del socio (Figma, sección 14). */
public record BeneficioResumenResponse(

        String id,
        String comercioId,
        String comercioNombre,
        String comercioRubro,
        String titulo,
        String descripcion,
        String tipoBeneficioId,
        String tipoBeneficioNombre,
        String valor,
        LocalDate fechaFinVigencia

) {

    public static BeneficioResumenResponse from(Beneficio beneficio) {
        return new BeneficioResumenResponse(
                beneficio.getId(),
                beneficio.getComercioId(),
                beneficio.getComercioNombre(),
                beneficio.getComercioRubro(),
                beneficio.getTitulo(),
                beneficio.getDescripcion(),
                beneficio.getTipoBeneficioId(),
                beneficio.getTipoBeneficioNombre(),
                beneficio.getValor(),
                beneficio.getFechaFinVigencia());
    }
}
