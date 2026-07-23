package com.almoby.ruralcuruzu.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Limitador de intentos genérico, en memoria (no requiere Redis ni nada externo).
 * Sirve para frenar fuerza bruta y spam: cada "clave" (una IP, un email, lo que sea)
 * tiene su propia ventana de tiempo con un contador de intentos.
 *
 * Limitación conocida: al vivir en memoria del proceso, el conteo se pierde si
 * la app se reinicia, y no se comparte entre varias instancias si el día de
 * mañana la app corre en más de un servidor. Para ese escenario habría que
 * migrar esto a algo compartido (ej. Redis). Para el tamaño actual del
 * proyecto, esto alcanza y evita sumar infraestructura de más.
 */
@Slf4j
@Component
public class RateLimiterService {

    private final Map<String, Ventana> ventanasPorClave = new ConcurrentHashMap<>();

    /**
     * @return true si el intento está permitido (y lo cuenta), false si ya se
     * superó el máximo de intentos para esa clave dentro de la ventana actual.
     */
    public boolean permitirIntento(String clave, int maxIntentos, Duration ventana) {
        Instant ahora = Instant.now();

        Ventana actual = ventanasPorClave.compute(clave, (k, existente) -> {
            if (existente == null || existente.expiraEn.isBefore(ahora)) {
                return new Ventana(ahora.plus(ventana), new AtomicInteger(0));
            }
            return existente;
        });

        int intentos = actual.contador.incrementAndGet();
        boolean permitido = intentos <= maxIntentos;

        if (!permitido) {
            log.warn("Rate limit excedido para clave={} ({} intentos, máximo {})", clave, intentos, maxIntentos);
        }

        return permitido;
    }

    /**
     * Limpieza periódica para no acumular claves viejas indefinidamente en memoria.
     */
    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    void limpiarVentanasExpiradas() {
        Instant ahora = Instant.now();
        int tamañoAntes = ventanasPorClave.size();
        ventanasPorClave.entrySet().removeIf(entry -> entry.getValue().expiraEn.isBefore(ahora));
        int eliminadas = tamañoAntes - ventanasPorClave.size();
        if (eliminadas > 0) {
            log.debug("Rate limiter: {} ventanas expiradas eliminadas de memoria", eliminadas);
        }
    }

    private record Ventana(Instant expiraEn, AtomicInteger contador) {
    }
}
