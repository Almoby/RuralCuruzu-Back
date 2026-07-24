package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;

import com.almoby.ruralcuruzu.domain.EjecucionGeneracionCuotas;
import com.almoby.ruralcuruzu.enums.OrigenEjecucionCuotas;

/** Resultado de una corrida de generación de cuotas (automática o manual). */
public record GeneracionCuotasResponse(

        String mensaje,
        String periodo,
        OrigenEjecucionCuotas origen,
        int cantidadSociosActivos,
        int cantidadCuotasGeneradas,
        int cantidadSociosOmitidos,
        Instant fechaEjecucion

) {

    public static GeneracionCuotasResponse from(EjecucionGeneracionCuotas ejecucion) {
        return new GeneracionCuotasResponse(
                "Generación de cuotas ejecutada con éxito",
                ejecucion.getPeriodo(),
                ejecucion.getOrigen(),
                ejecucion.getCantidadSociosActivos(),
                ejecucion.getCantidadCuotasGeneradas(),
                ejecucion.getCantidadSociosOmitidos(),
                ejecucion.getFechaEjecucion());
    }
}
