package com.almoby.ruralcuruzu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de la petición para cambiar la propia contraseña estando autenticado
 * (típicamente el primer ingreso de un Socio, con la contraseña temporal que
 * le llegó por correo al aprobarse su solicitud). Distinto del flujo de
 * recuperación por email (forgot-password/reset-password): acá no hace falta
 * ningún token porque la sesión ya prueba quién es.
 */
public record CambiarPasswordRequest(

        @Schema(description = "Contraseña actual (la temporal, en el caso del primer ingreso)")
        @NotBlank(message = "La contraseña actual es obligatoria")
        String passwordActual,

        @Schema(description = "Nueva contraseña elegida (mínimo 8 caracteres, no puede ser igual a la anterior)",
                example = "NuevaPassword123!")
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String passwordNueva

) {
}
