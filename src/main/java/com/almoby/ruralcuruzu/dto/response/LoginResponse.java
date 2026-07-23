package com.almoby.ruralcuruzu.dto.response;

import com.almoby.ruralcuruzu.domain.Rol;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta del login. El frontend usa {@code rol} para redirigir al
 * portal correspondiente (Socio, Comercio o Admin) y {@code requiereCambioPassword}
 * para forzar la pantalla de cambio de contraseña cuando corresponda (RN-16).
 */
public record LoginResponse(

        @Schema(description = "Access token (JWT). Va en el header 'Authorization: Bearer <token>' de los "
                + "requests a rutas protegidas. Dura poco (60 min por defecto).")
        String token,

        @Schema(description = "Siempre 'Bearer'. Indica el prefijo a usar en el header Authorization.")
        String tipoToken,

        @Schema(description = "Token de larga duración (7 días) para pedir un access token nuevo en "
                + "/api/auth/refresh sin volver a loguearse. Sirve una sola vez: cada uso devuelve uno nuevo.")
        String refreshToken,

        @Schema(description = "Rol de la cuenta: determina a qué portal redirige el frontend.")
        Rol rol,

        @Schema(description = "Nombre para mostrar en la interfaz.")
        String nombre,

        @Schema(description = "Id del perfil de Socio o Comercio asociado. Null si el rol es ADMIN.")
        String refId,

        @Schema(description = "Segundos que le quedan de vida al access token a partir de este momento.")
        long expiraEnSegundos,

        @Schema(description = "Si es true, el frontend debe forzar una pantalla de cambio de contraseña "
                + "antes de dejar usar el resto de la app (cuenta creada con contraseña temporal).")
        boolean requiereCambioPassword
) {
    public static LoginResponse bearer(String token, String refreshToken, Rol rol, String nombre, String refId,
                                        long expiraEnSegundos, boolean requiereCambioPassword) {
        return new LoginResponse(token, "Bearer", refreshToken, rol, nombre, refId, expiraEnSegundos, requiereCambioPassword);
    }
}
