package com.almoby.ruralcuruzu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cuerpo opcional del logout. Si el cliente manda su refreshToken, también
 * se revoca (así esa sesión queda completamente cerrada); si no lo manda,
 * el refresh token simplemente expira solo más adelante por su TTL.
 */
public record LogoutRequest(

        @Schema(description = "Opcional: si se manda, también se revoca (recomendado para cerrar la "
                + "sesión completa). Si se omite, el refresh token queda vivo hasta que expire solo.")
        String refreshToken

) {
}
