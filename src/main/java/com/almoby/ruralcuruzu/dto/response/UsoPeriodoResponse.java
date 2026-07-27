package com.almoby.ruralcuruzu.dto.response;

/** Cantidad de usos de beneficios de un comercio en un período (formato "yyyy-MM"). */
public record UsoPeriodoResponse(

        String periodo,
        long cantidad

) {
}
