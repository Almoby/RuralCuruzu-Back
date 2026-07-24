package com.almoby.ruralcuruzu.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.almoby.ruralcuruzu.enums.MedioPago;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** El socio informa (autoservicio) que pagó una cuota; queda a la espera de revisión. */
public record InformarPagoCuotaRequest(

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
