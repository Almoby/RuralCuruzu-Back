package com.almoby.ruralcuruzu.dto.response;

import java.util.List;

import com.almoby.ruralcuruzu.enums.EstadoComercio;

/**
 * Todo lo que necesita la pantalla "Inicio" del portal de comercio en una
 * sola llamada: el estado de su propia cuenta, las 4 tarjetas de
 * indicadores y la serie semanal del gráfico. Se agrupan en un único
 * endpoint (en vez de uno por widget) para no pagar un round-trip HTTP
 * extra por algo que ya se calcula sobre las mismas consultas a
 * HistorialBeneficio.
 */
public record InicioComercioResponse(

        EstadoComercio estado,
        IndicadoresComercioResponse indicadores,
        List<UsoDiaSemanaResponse> usosPorDia

) {
}
