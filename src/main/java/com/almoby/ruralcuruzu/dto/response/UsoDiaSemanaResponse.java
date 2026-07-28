package com.almoby.ruralcuruzu.dto.response;

import java.time.DayOfWeek;

/**
 * Fila del gráfico "Usos de beneficios esta semana" del portal de comercio.
 * Siempre trae los 7 días (lunes a domingo) de la semana en curso, con 0 en
 * los que todavía no tienen usos, para que el front no tenga que rellenar
 * huecos.
 */
public record UsoDiaSemanaResponse(

        DayOfWeek dia,
        long cantidad

) {
}
