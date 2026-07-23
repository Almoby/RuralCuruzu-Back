package com.almoby.ruralcuruzu.exception;

/**
 * Se lanza cuando se supera el límite de intentos permitidos en una
 * ventana de tiempo (ver RateLimiterService), por ejemplo demasiados
 * POST /api/auth/login desde la misma IP en poco tiempo.
 */
public class DemasiadasSolicitudesException extends RuntimeException {

    public DemasiadasSolicitudesException() {
        super("Demasiadas solicitudes en poco tiempo. Esperá unos minutos e intentá de nuevo");
    }
}
