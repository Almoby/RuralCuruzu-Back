package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;

/** Una fila de la tabla "Detalle de consumos recientes" de Estadísticas del comercio. */
public record ConsumoRecienteResponse(

        String socioNombre,
        String beneficioTitulo,
        Instant fechaUso

) {
}
