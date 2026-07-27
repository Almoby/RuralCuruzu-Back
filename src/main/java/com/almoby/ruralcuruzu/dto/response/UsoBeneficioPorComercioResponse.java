package com.almoby.ruralcuruzu.dto.response;

import java.util.List;

/**
 * Fila de "Uso de beneficios por comercio" (documento, sección 7.4). Se
 * calcula sobre HistorialBeneficio en estado USADO únicamente (lo ANULADO no
 * cuenta como uso real). Ordenado de mayor a menor cantidad de usos
 * (histórico total).
 *
 * <p>{@code cantidadBeneficiosUtilizados} es el acumulado histórico completo;
 * {@code cantidadBeneficiosUtilizadosEsteMes} es el mismo dato pero acotado
 * al mes en curso, para la vista "(mes actual)" de la pantalla de Reportes —
 * ya viene calculado para que el front no tenga que derivarlo sumando
 * {@code usoPorPeriodo} a mano.
 */
public record UsoBeneficioPorComercioResponse(

        String comercioId,
        String comercioNombre,
        long cantidadBeneficiosUtilizados,
        long cantidadBeneficiosUtilizadosEsteMes,
        long cantidadSociosUnicos,
        String promocionMasUtilizada,
        List<UsoPeriodoResponse> usoPorPeriodo

) {
}
