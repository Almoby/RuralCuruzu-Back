package com.almoby.ruralcuruzu.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoTipoCuota;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Alta de un tipo de cuota (documento, sección 10.1). */
public record CrearTipoCuotaRequest(

        @Schema(example = "Cuota de socio activo")
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @Schema(example = "Cuota mensual para socios de categoría activa")
        String descripcion,

        @NotNull(message = "La categoría aplicable es obligatoria")
        CategoriaSocio categoriaAplicable,

        @Schema(example = "15000.00")
        @NotNull(message = "El importe es obligatorio")
        @Positive(message = "El importe debe ser mayor a cero")
        BigDecimal importe,

        @Schema(description = "Fecha desde la cual aplica este importe", example = "2026-08-01")
        @NotNull(message = "La fecha de vigencia es obligatoria")
        LocalDate fechaVigencia,

        @Schema(description = "Día del mes en que vence (1-31)", example = "10")
        @NotNull(message = "El día de vencimiento es obligatorio")
        @Min(value = 1, message = "El día de vencimiento debe ser entre 1 y 31")
        @Max(value = 31, message = "El día de vencimiento debe ser entre 1 y 31")
        Integer diaVencimiento,

        @Schema(description = "Por defecto ACTIVO si no se especifica")
        EstadoTipoCuota estado

) {
}
