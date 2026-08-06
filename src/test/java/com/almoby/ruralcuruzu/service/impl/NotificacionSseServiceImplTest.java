package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.almoby.ruralcuruzu.domain.Notificacion;
import com.almoby.ruralcuruzu.dto.response.NotificacionResponse;
import com.almoby.ruralcuruzu.enums.ResultadoNotificacion;
import com.almoby.ruralcuruzu.enums.TipoNotificacion;

/**
 * NotificacionSseServiceImpl no tiene test container real detrás (no hay un
 * request HTTP de verdad), así que estos tests se apoyan en que SseEmitter
 * bufferea los send() hechos antes de que el handler del servlet se
 * inicialice, en vez de tirar una excepción. Lo que se verifica acá es
 * nuestra lógica (que suscribir/enviar/limpieza no exploten y devuelvan lo
 * esperado), no el comportamiento interno de SseEmitter en sí, que ya es
 * responsabilidad de Spring.
 */
class NotificacionSseServiceImplTest {

    private final NotificacionSseServiceImpl service = new NotificacionSseServiceImpl();

    private NotificacionResponse notificacionDePrueba() {
        Notificacion notificacion = Notificacion.builder()
                .id("notif-1")
                .tipo(TipoNotificacion.CUOTA_GENERADA)
                .asunto("Cuota generada")
                .mensaje("Se generó tu cuota de 2026-07")
                .resultado(ResultadoNotificacion.EXITOSO)
                .leida(false)
                .build();
        return NotificacionResponse.from(notificacion);
    }

    @Test
    void suscribir_devuelveEmitterSinTimeoutPropio() {
        SseEmitter emitter = service.suscribir("usuario-1");

        assertThat(emitter).isNotNull();
        // Se corta por desconexión/error del cliente, no por timeout fijo del servidor.
        assertThat(emitter.getTimeout()).isEqualTo(0L);
    }

    @Test
    void enviar_aUsuarioSinConexionAbierta_esNoOpYNoLanza() {
        assertThatCode(() -> service.enviar("usuario-sin-conexion", notificacionDePrueba()))
                .doesNotThrowAnyException();
    }

    @Test
    void enviar_aUsuarioConectado_noLanzaExcepcion() {
        service.suscribir("usuario-1");

        assertThatCode(() -> service.enviar("usuario-1", notificacionDePrueba()))
                .doesNotThrowAnyException();
    }

    @Test
    void enviar_aUsuarioConVariasConexiones_noLanzaExcepcion() {
        // Simula al mismo socio conectado desde dos pestañas/dispositivos a la vez.
        service.suscribir("usuario-1");
        service.suscribir("usuario-1");

        assertThatCode(() -> service.enviar("usuario-1", notificacionDePrueba()))
                .doesNotThrowAnyException();
    }

    @Test
    void completarEmitter_disparaLimpiezaSinExcepcion() {
        SseEmitter emitter = service.suscribir("usuario-1");

        assertThatCode(emitter::complete).doesNotThrowAnyException();

        // Tras completarse, un envío posterior a ese usuario no debería explotar
        // (la conexión ya fue removida del registro interno).
        assertThatCode(() -> service.enviar("usuario-1", notificacionDePrueba()))
                .doesNotThrowAnyException();
    }

    @Test
    void emitterConError_disparaLimpiezaSinExcepcion() {
        SseEmitter emitter = service.suscribir("usuario-1");

        assertThatCode(() -> emitter.completeWithError(new IllegalStateException("conexión caída")))
                .doesNotThrowAnyException();
    }
}
