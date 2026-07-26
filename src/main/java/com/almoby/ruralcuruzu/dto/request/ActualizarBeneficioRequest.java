package com.almoby.ruralcuruzu.dto.request;

import java.time.LocalDate;

import com.almoby.ruralcuruzu.enums.TipoBeneficio;
import com.almoby.ruralcuruzu.validation.RangoDeVigencia;
import com.almoby.ruralcuruzu.validation.RangoDeVigenciaValido;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Edición de un beneficio propio. */
@RangoDeVigenciaValido
public record ActualizarBeneficioRequest(

        @NotBlank(message = "El título es obligatorio")
        String titulo,

        String descripcion,

        @NotNull(message = "El tipo de beneficio es obligatorio")
        TipoBeneficio tipo,

        @NotBlank(message = "El valor es obligatorio")
        String valor,

        @Schema(description = "Null: vigente desde ya")
        LocalDate fechaInicioVigencia,

        @Schema(description = "Null: sin fecha de vencimiento")
        LocalDate fechaFinVigencia

) implements RangoDeVigencia {
}
