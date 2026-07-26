package com.almoby.ruralcuruzu.dto.request;

import com.almoby.ruralcuruzu.enums.EstadoBeneficio;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoBeneficioRequest(

        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoBeneficio nuevoEstado

) {
}
