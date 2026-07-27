package com.almoby.ruralcuruzu.dto.response;

import java.util.List;

/**
 * Fila de "Uso de beneficios por comercio" (documento, sección 7.4). Se
 * calcula sobre HistorialBeneficio en estado USADO únicamente (lo ANULADO no
 * cuenta como uso real). Ordenado de mayor a menor cantidad de usos.
 */
public record UsoBeneficioPorComercioResponse(

        String comercioId,
        String comercioNombre,
        long cantidadBeneficiosUtilizados,
        long cantidadSociosUnicos,
        String promocionMasUtilizada,
        List<UsoPeriodoResponse> usoPorPeriodo

) {
}
