package com.almoby.ruralcuruzu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de la petición para completar la recuperación de contraseña:
 * el token recibido por email más la nueva contraseña elegida.
 */
public record ResetPasswordRequest(

        @Schema(description = "Token que llegó por email (el valor del parámetro ?token= del enlace)")
        @NotBlank(message = "El token es obligatorio")
        String token,

        @Schema(description = "Nueva contraseña elegida (mínimo 8 caracteres, no puede ser igual a la anterior)",
                example = "NuevaPassword123!")
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String nuevaPassword

) {
}
