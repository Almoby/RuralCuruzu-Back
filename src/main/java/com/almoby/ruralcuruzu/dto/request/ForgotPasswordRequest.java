package com.almoby.ruralcuruzu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de la petición para iniciar la recuperación de contraseña.
 */
public record ForgotPasswordRequest(

        @Schema(description = "Email de la cuenta que quiere recuperar su contraseña", example = "socio@ruralcuruzu.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email

) {
}
