package com.almoby.ruralcuruzu.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.almoby.ruralcuruzu.dto.response.NotificacionResponse;

/**
 * Push en tiempo real de notificaciones nuevas (mejora futura pedida por el
 * front: "Notificaciones en tiempo real (WebSocket/SSE)"). Complementa al
 * polling que ya existía (GET /api/notificaciones, GET .../no-leidas/contador):
 * ese sigue funcionando igual, esto es un empujón adicional para que la
 * campanita se actualice sin esperar al próximo poll.
 */
public interface NotificacionSseService {

    /** Abre una conexión SSE para el usuario autenticado; el front la mantiene abierta con EventSource. */
    SseEmitter suscribir(String usuarioId);

    /** Empuja una notificación a todas las conexiones abiertas de ese usuario (0, 1 o varias pestañas/dispositivos). */
    void enviar(String usuarioId, NotificacionResponse notificacion);
}
