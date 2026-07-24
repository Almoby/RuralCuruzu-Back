package com.almoby.ruralcuruzu.dto.request;

import java.time.LocalDate;
import java.util.List;

import com.almoby.ruralcuruzu.enums.MedioPago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Registro manual de un pago hecho por el admin (documento, sección 10.4;
 * ajustado al Figma). El admin elige un socio y uno o varios períodos
 * (multi-select de meses, ej. pagar agosto + septiembre + octubre juntos, o
 * un año entero): cada período debe tener ya una cuota generada para ese
 * socio, y todas quedan PAGADA con los mismos datos de pago.
 *
 * <p>No se pide el importe acá: cada cuota ya tiene su propio importe fijado
 * al generarse (documento 10.2, tomado de la regla de cuota vigente para la
 * categoría del socio en ese momento), y ese es el monto que se registra
 * como pagado. Así se evita que el admin pueda tipear un monto arbitrario
 * que no coincida con lo realmente adeudado.
 */
public record RegistrarPagoCuotaRequest(

        @Schema(description = "Id del socio que está pagando", example = "6a62acadcaa52614a84e907f")
        @NotBlank(message = "El socio es obligatorio")
        String socioId,

        @Schema(description = "Períodos que se están pagando, formato yyyy-MM (uno o varios)",
                example = "[\"2026-08\", \"2026-09\", \"2026-10\"]")
        @NotEmpty(message = "Hay que indicar al menos un período")
        List<String> periodos,

        @NotNull(message = "La fecha de pago es obligatoria")
        LocalDate fecha,

        @NotNull(message = "El medio de pago es obligatorio")
        MedioPago medioPago,

        String comprobante,

        String observacion

) {
}
