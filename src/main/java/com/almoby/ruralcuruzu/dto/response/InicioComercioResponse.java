package com.almoby.ruralcuruzu.dto.response;

import java.util.List;

/**
 * Todo lo que necesita la pantalla "Inicio" del portal de comercio en una
 * sola llamada: las 4 tarjetas de indicadores y la serie semanal del
 * gráfico. Se agrupan en un único endpoint (en vez de uno por widget) para
 * no pagar un round-trip HTTP extra por algo que ya se calcula sobre las
 * mismas consultas a HistorialBeneficio.
 */
public record InicioComercioResponse(

        IndicadoresComercioResponse indicadores,
        List<UsoDiaSemanaResponse> usosPorDia

) {
}
