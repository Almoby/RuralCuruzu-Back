package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.Notificacion;
import com.almoby.ruralcuruzu.dto.response.ContadorNoLeidasResponse;
import com.almoby.ruralcuruzu.dto.response.NotificacionResponse;
import com.almoby.ruralcuruzu.exception.NotificacionNoEncontradaException;
import com.almoby.ruralcuruzu.repository.NotificacionRepository;
import com.almoby.ruralcuruzu.service.NotificacionService;

/** Ver NotificacionService. */
@Service
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionServiceImpl(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    public List<NotificacionResponse> listarPropias(String usuarioId) {
        return notificacionRepository.findByDestinatarioIdOrderByFechaEnvioDesc(usuarioId).stream()
                .map(NotificacionResponse::from)
                .toList();
    }

    @Override
    public ContadorNoLeidasResponse contarNoLeidas(String usuarioId) {
        return new ContadorNoLeidasResponse(notificacionRepository.countByDestinatarioIdAndLeidaFalse(usuarioId));
    }

    @Override
    public void marcarLeida(String notificacionId, String usuarioId) {
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .filter(n -> usuarioId.equals(n.getDestinatarioId()))
                .orElseThrow(() -> new NotificacionNoEncontradaException(notificacionId));

        if (!notificacion.isLeida()) {
            notificacion.setLeida(true);
            notificacion.setFechaLectura(Instant.now());
            notificacionRepository.save(notificacion);
        }
    }
}
