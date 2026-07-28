package com.almoby.ruralcuruzu.dto.response;

import java.util.List;

/**
 * Pantalla "Estadísticas" del comercio: indicadores histórico + de este mes,
 * serie mensual del año consultado, uso por promoción de este mes y detalle
 * de los últimos consumos. Todo en una sola llamada.
 */
public record EstadisticasComercioResponse(

        IndicadoresEstadisticasComercioResponse indicadores,
        List<UsoMensualResponse> usosMensuales,
        List<UsoPorPromocionResponse> usosPorPromocion,
        List<ConsumoRecienteResponse> consumosRecientes

) {
}
