package com.almoby.ruralcuruzu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Alta de un tipo de beneficio en el catálogo administrable (admin). */
public record CrearTipoBeneficioRequest(

        @Schema(description = "Identificador legible y estable, en mayúsculas", example = "DESCUENTO_PORCENTAJE")
        @NotBlank(message = "El código es obligatorio")
        String codigo,

        @Schema(example = "Descuento por porcentaje")
        @NotBlank(message = "El nombre es obligatorio")
        String nombre

) {
}
