package com.almoby.ruralcuruzu.dto.request;

import com.almoby.ruralcuruzu.enums.EstadoSocio;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoSocioRequest(

        @Schema(description = "Nuevo estado del socio")
        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoSocio nuevoEstado

) {
}
