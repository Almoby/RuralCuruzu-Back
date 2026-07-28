package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;

import com.almoby.ruralcuruzu.domain.Notificacion;
import com.almoby.ruralcuruzu.enums.ResultadoNotificacion;
import com.almoby.ruralcuruzu.enums.TipoNotificacion;

/** Una notificación tal como la ve el destinatario en su campanita. */
public record NotificacionResponse(

        String id,
        TipoNotificacion tipo,
        String asunto,
        String mensaje,
        ResultadoNotificacion resultado,
        boolean leida,
        Instant fechaEnvio

) {

    public static NotificacionResponse from(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getTipo(),
                notificacion.getAsunto(),
                notificacion.getMensaje(),
                notificacion.getResultado(),
                notificacion.isLeida(),
                notificacion.getFechaEnvio());
    }
}
