package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;

/**
 * Un punto del gráfico de barras de cobranza mensual (documento, sección
 * 7.2): lo cobrado (cuotas PAGADA) contra lo pendiente (PENDIENTE, VENCIDA,
 * INFORMADA o EN_REVISION) de ese período, dentro del año consultado.
 */
public record CobranzaMensualResponse(

        String periodo,
        String mes,
        BigDecimal cobrado,
        BigDecimal pendiente

) {
}
