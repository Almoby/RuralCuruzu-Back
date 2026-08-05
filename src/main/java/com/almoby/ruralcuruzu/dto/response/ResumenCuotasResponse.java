package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Totales para las tarjetas y pestañas del panel de "Gestión de Cuotas"
 * (Figma): montos cobrados y cantidades por estado, ya calculados, para que
 * el front no tenga que traer todo el listado y sumarlo del lado suyo.
 */
public record ResumenCuotasResponse(

        BigDecimal totalCobrado,
        BigDecimal totalEnRevision,
        BigDecimal totalCobradoEnEfectivo,

        long cantidadTodas,
        long cantidadPendientes,
        long cantidadAprobadas,
        long cantidadRechazadas,

        List<CobranzaPorCategoriaResponse> cobranzaPorCategoria

) {
}
