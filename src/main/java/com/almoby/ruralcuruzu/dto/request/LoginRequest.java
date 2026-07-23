package com.almoby.ruralcuruzu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de la petición de login. Es el mismo endpoint para los tres roles:
 * el rol se resuelve del lado del servidor a partir del usuario encontrado,
 * el cliente nunca lo envía.
 */
public record LoginRequest(

        @Schema(description = "Email de la cuenta", example = "socio@ruralcuruzu.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @Schema(description = "Contraseña de la cuenta", example = "MiPassword123!")
        @NotBlank(message = "La contraseña es obligatoria")
        String password

) {
}
