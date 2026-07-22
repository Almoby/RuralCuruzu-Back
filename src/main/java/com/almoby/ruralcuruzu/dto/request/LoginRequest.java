package com.almoby.ruralcuruzu.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de la petición de login. Es el mismo endpoint para los tres roles:
 * el rol se resuelve del lado del servidor a partir del usuario encontrado,
 * el cliente nunca lo envía.
 */
public record LoginRequest(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password

) {
}
