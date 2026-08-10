package com.almoby.ruralcuruzu.dto.request;

import java.time.LocalDate;

import com.almoby.ruralcuruzu.validation.RangoDeVigencia;
import com.almoby.ruralcuruzu.validation.RangoDeVigenciaValido;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Edición de un beneficio propio. */
@RangoDeVigenciaValido
public record ActualizarBeneficioRequest(

        @NotBlank(message = "El título es obligatorio")
        String titulo,

        String descripcion,

        @Schema(description = "Id de un tipo activo del catálogo (GET /api/tipos-beneficio)")
        @NotBlank(message = "El tipo de beneficio es obligatorio")
        String tipoBeneficioId,

        @NotBlank(message = "El valor es obligatorio")
        String valor,

        @Schema(description = "Null: vigente desde ya")
        LocalDate fechaInicioVigencia,

        @Schema(description = "Null: sin fecha de vencimiento")
        LocalDate fechaFinVigencia,

        @Schema(description = "Cuántas veces puede usar este beneficio el mismo socio, para siempre. Si se "
                + "omite, queda limitado a 1 uso. 0 = sin límite (ilimitado). Cualquier entero positivo "
                + "(1, 2, 3...) = ese límite exacto. Bajar el límite no afecta los usos ya canjeados, solo "
                + "los que falten.", example = "1")
        @PositiveOrZero(message = "El límite de usos no puede ser negativo")
        Integer limiteUsosPorSocio

) implements RangoDeVigencia {
}
