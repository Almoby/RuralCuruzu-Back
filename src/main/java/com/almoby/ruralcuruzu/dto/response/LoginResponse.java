package com.almoby.ruralcuruzu.dto.response;

import com.almoby.ruralcuruzu.domain.Rol;

/**
 * Respuesta del login. El frontend usa {@code rol} para redirigir al
 * portal correspondiente (Socio, Comercio o Admin) y {@code requiereCambioPassword}
 * para forzar la pantalla de cambio de contraseña cuando corresponda (RN-16).
 */
public record LoginResponse(
        String token,
        String tipoToken,
        Rol rol,
        String nombre,
        String refId,
        long expiraEnSegundos,
        boolean requiereCambioPassword
) {
    public static LoginResponse bearer(String token, Rol rol, String nombre, String refId,
                                        long expiraEnSegundos, boolean requiereCambioPassword) {
        return new LoginResponse(token, "Bearer", rol, nombre, refId, expiraEnSegundos, requiereCambioPassword);
    }
}
