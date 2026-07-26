package com.almoby.ruralcuruzu.dto.response;

import java.time.LocalDate;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.enums.TipoBeneficio;

/** Fila para el listado de "Beneficios y Comercios" del socio (Figma, sección 14). */
public record BeneficioResumenResponse(

        String id,
        String comercioId,
        String comercioNombre,
        String comercioRubro,
        String titulo,
        String descripcion,
        TipoBeneficio tipo,
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
                beneficio.getTipo(),
                beneficio.getValor(),
                beneficio.getFechaFinVigencia());
    }
}
