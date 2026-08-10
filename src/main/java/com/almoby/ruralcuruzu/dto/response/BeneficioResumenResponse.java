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
        LocalDate fechaFinVigencia,
        /** Normalizado (nunca null): 1 = default legado, 0 = ilimitado. */
        int limiteUsosPorSocio,
        /** Cuántas veces ya usó ESTE socio (el que hace la consulta) este beneficio. */
        long usosDelSocio,
        /** null = ilimitado (limiteUsosPorSocio == 0). */
        Integer usosRestantes

) {

    /** Sin dato del socio (uso interno/legado): queda en 0 usos. */
    public static BeneficioResumenResponse from(Beneficio beneficio) {
        return from(beneficio, 0L);
    }

    public static BeneficioResumenResponse from(Beneficio beneficio, long usosDelSocio) {
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
                beneficio.getFechaFinVigencia(),
                beneficio.limiteUsosPorSocioEfectivo(),
                usosDelSocio,
                beneficio.usosRestantes(usosDelSocio));
    }
}
