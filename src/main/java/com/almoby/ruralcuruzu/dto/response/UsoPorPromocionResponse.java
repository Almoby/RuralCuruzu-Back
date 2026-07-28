package com.almoby.ruralcuruzu.dto.response;

/**
 * Una barra del gráfico "Usos por promoción (este mes)": solo aparecen las
 * promociones del comercio que tuvieron al menos un uso este mes (no se
 * rellena con 0 las que no tuvieron ninguno).
 */
public record UsoPorPromocionResponse(

        String beneficioId,
        String beneficioTitulo,
        long cantidad

) {
}
