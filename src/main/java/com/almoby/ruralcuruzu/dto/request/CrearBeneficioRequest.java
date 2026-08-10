package com.almoby.ruralcuruzu.dto.request;

import java.time.LocalDate;

import com.almoby.ruralcuruzu.validation.RangoDeVigencia;
import com.almoby.ruralcuruzu.validation.RangoDeVigenciaValido;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Alta de un beneficio, hecha por el propio comercio (documento, sección 14). */
@RangoDeVigenciaValido
public record CrearBeneficioRequest(

        @Schema(example = "15% en medicamentos")
        @NotBlank(message = "El título es obligatorio")
        String titulo,

        @Schema(example = "Descuento en toda la línea de medicamentos de venta libre")
        String descripcion,

        @Schema(description = "Id de un tipo activo del catálogo (GET /api/tipos-beneficio)", example = "665f1a2b3c4d5e6f7a8b9c0d")
        @NotBlank(message = "El tipo de beneficio es obligatorio")
        String tipoBeneficioId,

        @Schema(description = "Texto para mostrar en la tarjeta del beneficio", example = "15%")
        @NotBlank(message = "El valor es obligatorio")
        String valor,

        @Schema(description = "Null: vigente desde ya")
        LocalDate fechaInicioVigencia,

        @Schema(description = "Null: sin fecha de vencimiento")
        LocalDate fechaFinVigencia,

        @Schema(description = "Cuántas veces puede usar este beneficio el mismo socio, para siempre (no se "
                + "resetea por día ni por mes). Si se omite, queda limitado a 1 uso. 0 = sin límite (ilimitado). "
                + "Cualquier entero positivo (1, 2, 3...) = ese límite exacto.", example = "1")
        @PositiveOrZero(message = "El límite de usos no puede ser negativo")
        Integer limiteUsosPorSocio

) implements RangoDeVigencia {
}
