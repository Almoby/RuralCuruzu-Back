package com.almoby.ruralcuruzu.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.almoby.ruralcuruzu.dto.response.NotificacionResponse;
import com.almoby.ruralcuruzu.service.NotificacionSseService;

import lombok.extern.slf4j.Slf4j;

/**
 * Registro en memoria de conexiones SSE abiertas, por usuarioId. En memoria
 * a propósito (no en Mongo): es estado efímero de esta única instancia del
 * servidor, se pierde y se reconstruye solo con cada reconexión del cliente,
 * no hace falta persistirlo.
 *
 * Nota de escalabilidad: si el día de mañana el backend corre en más de una
 * instancia (varios pods/servidores detrás de un load balancer), un usuario
 * conectado a la instancia A no recibiría el push si la notificación se
 * generó procesando un request que cayó en la instancia B. El polling
 * existente (GET /api/notificaciones) sigue funcionando siempre igual, así
 * que no hay pérdida de datos, solo una demora hasta el próximo poll en ese
 * escenario. Resolverlo (ej. con un pub/sub compartido tipo Redis) queda
 * fuera de alcance mientras el despliegue sea de una sola instancia.
 */
@Slf4j
@Service
public class NotificacionSseServiceImpl implements NotificacionSseService {

    /** Sin límite de tiempo propio: se corta por error/desconexión del cliente, no por timeout fijo del servidor. */
    private static final long SIN_TIMEOUT = 0L;

    private final Map<String, List<SseEmitter>> emittersPorUsuario = new ConcurrentHashMap<>();

    @Override
    public SseEmitter suscribir(String usuarioId) {
        SseEmitter emitter = new SseEmitter(SIN_TIMEOUT);
        // compute() (no computeIfAbsent()+add() por separado) para que el alta quede
        // atómica junto con la baja de quitar(): ConcurrentHashMap serializa las
        // llamadas a la familia compute/computeIfPresent sobre la misma key, así que
        // esto evita que una desconexión que vacía y borra la entrada del mapa
        // "pise" a una suscripción nueva que llega casi al mismo tiempo.
        emittersPorUsuario.compute(usuarioId, (id, existentes) -> {
            List<SseEmitter> lista = existentes != null ? existentes : new CopyOnWriteArrayList<>();
            lista.add(emitter);
            return lista;
        });

        emitter.onCompletion(() -> quitar(usuarioId, emitter));
        emitter.onTimeout(() -> quitar(usuarioId, emitter));
        emitter.onError(ex -> quitar(usuarioId, emitter));

        try {
            // Evento inicial: confirma la conexión de entrada y evita que algunos
            // proxies intermedios corten una respuesta que todavía no mandó nada.
            emitter.send(SseEmitter.event().name("conectado").data("ok"));
        } catch (IOException ex) {
            quitar(usuarioId, emitter);
        }

        log.debug("Nueva conexión SSE para usuarioId={} (total abiertas: {})",
                usuarioId, emittersPorUsuario.getOrDefault(usuarioId, List.of()).size());
        return emitter;
    }

    @Override
    public void enviar(String usuarioId, NotificacionResponse notificacion) {
        List<SseEmitter> emitters = emittersPorUsuario.get(usuarioId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        // Copia defensiva: enviar() puede disparar quitar() (vía onError) sobre la
        // misma lista que estaríamos iterando.
        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name("notificacion").data(notificacion));
            } catch (IOException | IllegalStateException ex) {
                // IllegalStateException: el emitter ya se completó (ej. onCompletion todavía
                // no corrió porque el request nunca se llegó a inicializar del todo) pero
                // sigue en el registro; lo tratamos igual que una conexión caída.
                quitar(usuarioId, emitter);
            }
        }
    }

    private void quitar(String usuarioId, SseEmitter emitter) {
        emittersPorUsuario.computeIfPresent(usuarioId, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
