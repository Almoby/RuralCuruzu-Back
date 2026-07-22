package com.almoby.ruralcuruzu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de la petición para completar la recuperación de contraseña:
 * el token recibido por email más la nueva contraseña elegida.
 */
public record ResetPasswordRequest(

        @NotBlank(message = "El token es obligatorio")
        String token,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String nuevaPassword

) {
}
