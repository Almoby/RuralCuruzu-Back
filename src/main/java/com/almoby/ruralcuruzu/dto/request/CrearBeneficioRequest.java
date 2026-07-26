package com.almoby.ruralcuruzu.dto.request;

import java.time.LocalDate;

import com.almoby.ruralcuruzu.enums.TipoBeneficio;
import com.almoby.ruralcuruzu.validation.RangoDeVigencia;
import com.almoby.ruralcuruzu.validation.RangoDeVigenciaValido;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Alta de un beneficio, hecha por el propio comercio (documento, sección 14). */
@RangoDeVigenciaValido
public record CrearBeneficioRequest(

        @Schema(example = "15% en medicamentos")
        @NotBlank(message = "El título es obligatorio")
        String titulo,

        @Schema(example = "Descuento en toda la línea de medicamentos de venta libre")
        String descripcion,

        @NotNull(message = "El tipo de beneficio es obligatorio")
        TipoBeneficio tipo,

        @Schema(description = "Texto para mostrar en la tarjeta del beneficio", example = "15%")
        @NotBlank(message = "El valor es obligatorio")
        String valor,

        @Schema(description = "Null: vigente desde ya")
        LocalDate fechaInicioVigencia,

        @Schema(description = "Null: sin fecha de vencimiento")
        LocalDate fechaFinVigencia

) implements RangoDeVigencia {
}
