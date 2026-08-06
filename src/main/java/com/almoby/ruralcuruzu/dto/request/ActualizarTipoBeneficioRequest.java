package com.almoby.ruralcuruzu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Edición parcial de un tipo de beneficio: cada campo es opcional, solo se
 * pisa lo que venga distinto de null (mismo patrón que
 * ActualizarSocioParcialRequest). El código no se puede editar (es el
 * identificador estable); para eso hay que dar de baja y crear uno nuevo.
 */
public record ActualizarTipoBeneficioRequest(

        @Schema(example = "Descuento por porcentaje")
        String nombre,

        @Schema(description = "Solo los tipos activos aparecen en el dropdown para beneficios nuevos")
        Boolean activo

) {
}
