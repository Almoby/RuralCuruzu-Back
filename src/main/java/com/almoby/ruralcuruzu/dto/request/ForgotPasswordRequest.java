package com.almoby.ruralcuruzu.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de la petición para iniciar la recuperación de contraseña.
 */
public record ForgotPasswordRequest(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email

) {
}
