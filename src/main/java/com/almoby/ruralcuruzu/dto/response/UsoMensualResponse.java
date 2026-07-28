package com.almoby.ruralcuruzu.dto.response;

/**
 * Un punto del gráfico de línea "Usos mensuales (histórico)" de la pantalla
 * de Estadísticas del comercio: los 12 meses del año consultado, con 0 en
 * los meses sin usos (mismo criterio que {@link CobranzaMensualResponse}).
 */
public record UsoMensualResponse(

        String periodo,
        String mes,
        long cantidad

) {
}
