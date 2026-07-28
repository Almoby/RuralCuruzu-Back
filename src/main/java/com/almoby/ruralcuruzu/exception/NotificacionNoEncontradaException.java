package com.almoby.ruralcuruzu.exception;

/** No existe esa notificación, o no le pertenece a quien la pide (mismo criterio: no se distingue el motivo). */
public class NotificacionNoEncontradaException extends RuntimeException {

    public NotificacionNoEncontradaException(String id) {
        super("No existe ninguna notificación con id " + id + ", o no es propia");
    }
}
