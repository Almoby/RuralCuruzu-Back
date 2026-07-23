package com.almoby.ruralcuruzu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de la petición para renovar el access token sin volver a loguearse.
 */
public record RefreshTokenRequest(

        @Schema(description = "El refreshToken que devolvió el login (o un /refresh anterior)")
        @NotBlank(message = "El refreshToken es obligatorio")
        String refreshToken

) {
}
