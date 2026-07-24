package com.almoby.ruralcuruzu.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.almoby.ruralcuruzu.enums.MedioPago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Registro manual de un pago hecho por el admin (documento, sección 10.4).
 * Puede cubrir una o varias cuotas a la vez (ej. el socio paga dos meses
 * juntos): todas quedan PAGADA con los mismos datos de pago.
 */
public record RegistrarPagoCuotaRequest(

        @Schema(description = "Ids de la cuota o cuotas que se están pagando")
        @NotEmpty(message = "Hay que indicar al menos una cuota")
        List<String> cuotaIds,

        @NotNull(message = "La fecha de pago es obligatoria")
        LocalDate fecha,

        @NotNull(message = "El importe es obligatorio")
        @Positive(message = "El importe debe ser mayor a cero")
        BigDecimal importe,

        @NotNull(message = "El medio de pago es obligatorio")
        MedioPago medioPago,

        String comprobante,

        String observacion

) {
}
