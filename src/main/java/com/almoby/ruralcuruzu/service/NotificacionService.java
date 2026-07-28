package com.almoby.ruralcuruzu.service;

import java.util.List;

import com.almoby.ruralcuruzu.dto.response.ContadorNoLeidasResponse;
import com.almoby.ruralcuruzu.dto.response.NotificacionResponse;

/**
 * La campanita in-app (documento, sección 29 y el módulo de notificaciones
 * pendiente en el Figma): reutiliza el mismo registro que arma
 * {@link com.almoby.ruralcuruzu.service.impl.NotificacionEmailServiceImpl}
 * para cada correo (29.3), sin distinguir por rol - cualquier usuario
 * autenticado (socio, admin o comercio) ve y administra únicamente las
 * suyas.
 */
public interface NotificacionService {

    /** Más recientes primero. */
    List<NotificacionResponse> listarPropias(String usuarioId);

    ContadorNoLeidasResponse contarNoLeidas(String usuarioId);

    /**
     * @throws com.almoby.ruralcuruzu.exception.NotificacionNoEncontradaException
     *         si no existe, o no le pertenece al usuario indicado
     */
    void marcarLeida(String notificacionId, String usuarioId);
}
