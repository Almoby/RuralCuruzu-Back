package com.almoby.ruralcuruzu.dto.response;

/**
 * Tarjetas del "Inicio" del portal de comercio. {@code sociosAlcanzados} es
 * la cantidad de socios distintos que alguna vez canjearon un beneficio de
 * este comercio (histórico completo); el resto de los indicadores son del
 * período indicado en su nombre (mes o día actual).
 */
public record IndicadoresComercioResponse(

        long usosEsteMes,
        long promocionesActivas,
        long sociosAlcanzados,
        long validacionesHoy

) {
}
