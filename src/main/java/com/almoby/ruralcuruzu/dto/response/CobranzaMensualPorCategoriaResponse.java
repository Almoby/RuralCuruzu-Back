package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;

/**
 * Un punto del gráfico "Cuotas cobradas por mes de socios activos y
 * adherentes" (pantalla de Reportes): lo efectivamente cobrado (cuotas
 * PAGADA) de cada categoría de socio, mes a mes, dentro del año consultado.
 * Siempre trae las 12 filas del año (con 0 donde no hubo cobros), igual que
 * {@link CobranzaMensualResponse}.
 */
public record CobranzaMensualPorCategoriaResponse(

        String periodo,
        String mes,
        BigDecimal cobradoActivo,
        BigDecimal cobradoAdherente

) {
}
