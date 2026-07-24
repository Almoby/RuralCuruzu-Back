package com.almoby.ruralcuruzu.dto.request;

import com.almoby.ruralcuruzu.enums.EstadoTipoCuota;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoTipoCuotaRequest(

        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoTipoCuota nuevoEstado

) {
}
