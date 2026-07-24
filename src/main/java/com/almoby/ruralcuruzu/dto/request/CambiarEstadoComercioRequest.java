package com.almoby.ruralcuruzu.dto.request;

import com.almoby.ruralcuruzu.enums.EstadoComercio;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoComercioRequest(

        @Schema(description = "Nuevo estado del comercio")
        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoComercio nuevoEstado

) {
}
