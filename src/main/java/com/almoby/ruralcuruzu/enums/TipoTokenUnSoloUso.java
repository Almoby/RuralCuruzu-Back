package com.almoby.ruralcuruzu.enums;

/**
 * Distingue para qué flujo se generó un {@link com.almoby.ruralcuruzu.domain.TokenUnSoloUso}.
 * El mismo mecanismo (hash SHA-256, TTL, un solo uso) sirve para cualquier
 * link de un solo uso que se manda por correo; lo que cambia entre flujos es
 * a qué entidad apunta el token (ownerId) y cuánto dura.
 */
public enum TipoTokenUnSoloUso {

    /** ownerId = usuarioId. Flujo "olvidé mi contraseña". */
    RESET_PASSWORD,

    /** ownerId = numeroSolicitud. Responder una observación de SolicitudSocio sin cuenta. */
    RESPUESTA_SOLICITUD
}
