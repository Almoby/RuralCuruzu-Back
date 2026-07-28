package com.almoby.ruralcuruzu.dto.response;

/**
 * Las 4 tarjetas de arriba de la pantalla "Estadísticas" del comercio.
 * A diferencia de {@link IndicadoresComercioResponse} (pantalla "Inicio"),
 * acá el uso principal es el histórico total, no el de hoy.
 */
public record IndicadoresEstadisticasComercioResponse(

        long usosHistoricoTotal,
        long sociosUnicos,
        long promocionesActivas,
        long usosEsteMes

) {
}
