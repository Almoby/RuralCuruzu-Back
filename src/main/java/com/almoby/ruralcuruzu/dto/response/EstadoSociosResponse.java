package com.almoby.ruralcuruzu.dto.response;

/**
 * Gráfico circular "Estado de socios" (documento, sección 7.3). "Inactivos"
 * son los socios con EstadoSocio distinto de ACTIVO; los otros tres se
 * calculan solo sobre los socios ACTIVOS, mirando sus cuotas: vencido si
 * tiene alguna cuota VENCIDA, pendiente si tiene alguna PENDIENTE/INFORMADA/
 * EN_REVISION (y ninguna vencida), al día en cualquier otro caso (incluido
 * no tener cuotas todavía).
 */
public record EstadoSociosResponse(

        long alDia,
        long pendientes,
        long vencidos,
        long inactivos

) {
}
