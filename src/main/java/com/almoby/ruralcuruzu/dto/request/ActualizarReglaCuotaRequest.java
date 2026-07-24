package com.almoby.ruralcuruzu.dto.request;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Crea o actualiza (upsert) la regla de cuota de una categoría. La
 * categoría no va en el body: es parte de la URL
 * ({@code PUT /api/admin/reglas-cuota/{categoria}}).
 */
public record ActualizarReglaCuotaRequest(

        @Schema(example = "Cuota de socio activo")
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @Schema(example = "15000.00")
        @NotNull(message = "El importe es obligatorio")
        @Positive(message = "El importe debe ser mayor a cero")
        BigDecimal importe,

        @Schema(description = "Día del mes en que vence (1-31)", example = "10")
        @NotNull(message = "El día de vencimiento es obligatorio")
        @Min(value = 1, message = "El día de vencimiento debe ser entre 1 y 31")
        @Max(value = 31, message = "El día de vencimiento debe ser entre 1 y 31")
        Integer diaVencimiento

) {
}
