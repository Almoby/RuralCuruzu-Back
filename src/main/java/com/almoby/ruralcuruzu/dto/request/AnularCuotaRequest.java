package com.almoby.ruralcuruzu.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AnularCuotaRequest(

        @NotBlank(message = "El motivo es obligatorio para anular una cuota")
        String motivo

) {
}
