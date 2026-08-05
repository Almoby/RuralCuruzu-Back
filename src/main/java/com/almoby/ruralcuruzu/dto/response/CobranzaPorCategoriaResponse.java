package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;

import com.almoby.ruralcuruzu.enums.CategoriaSocio;

/**
 * Desglose del resumen de cuotas por categoría de socio (ACTIVO/ADHERENTE),
 * para el gráfico de "Reportes" del panel de Gestión de Cuotas. Siempre
 * trae una fila por cada valor de {@link CategoriaSocio} (aunque no tenga
 * ninguna cuota todavía, con los totales en cero), igual que
 * obtenerCobranzaMensual siempre trae los 12 meses del año.
 */
public record CobranzaPorCategoriaResponse(

        CategoriaSocio categoria,
        BigDecimal totalCobrado,
        long cantidadCuotas

) {
}
