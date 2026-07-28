package com.almoby.ruralcuruzu.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.service.CuotaService;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;

/**
 * Webhook público de Mercado Pago (documento 10.4, canal "link de pago").
 * Ruta pública (SecurityConfig): Mercado Pago no manda ningún token nuestro,
 * así que NUNCA hay que confiar en el contenido de esta notificación por sí
 * solo — solo dice "revisá este pago", y {@link CuotaService#procesarNotificacionMercadoPago}
 * siempre reconsulta el estado real contra la API de Mercado Pago antes de
 * tocar nada.
 *
 * Mercado Pago manda la notificación de dos formas (según la config de la
 * cuenta y la versión): querystring clásico ({@code ?type=payment&data.id=123})
 * o body JSON ({@code {"action":"payment.created","data":{"id":"123"}}}).
 * Se soportan las dos. Siempre hay que responder 200 rápido (si no, Mercado
 * Pago reintenta la notificación).
 *
 * NOTA: esto no puede probarse de punta a punta en este entorno sandboxeado:
 * hace falta una URL pública real (mercadopago.notification-url) y un
 * MERCADOPAGO_ACCESS_TOKEN real para que Mercado Pago pueda llamar acá y para
 * poder reconsultar el pago.
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.MERCADO_PAGO_WEBHOOK_BASE)
@Hidden
public class PagoWebhookController {

    private final CuotaService cuotaService;

    public PagoWebhookController(CuotaService cuotaService) {
        this.cuotaService = cuotaService;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> recibirNotificacion(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String topic,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestParam(name = "id", required = false) String idQueryParam,
            @RequestBody(required = false) Map<String, Object> body) {

        String tipoNotificacion = type != null ? type : topic;
        String paymentId = dataId != null ? dataId : idQueryParam;

        if (paymentId == null && body != null) {
            Object data = body.get("data");
            if (data instanceof Map<?, ?> dataMap && dataMap.get("id") != null) {
                paymentId = String.valueOf(dataMap.get("id"));
            }
        }

        // Mercado Pago manda notificaciones de otros "topics" además de pagos
        // (ej. merchant_order); las reconocemos con un 200 pero no hacemos nada.
        if (tipoNotificacion != null && !"payment".equalsIgnoreCase(tipoNotificacion)) {
            log.info("Notificación de Mercado Pago de tipo '{}' ignorada", tipoNotificacion);
            return ResponseEntity.ok().build();
        }

        if (paymentId == null) {
            log.warn("Notificación de Mercado Pago sin id de pago (type={}, body={}), se ignora",
                    tipoNotificacion, body);
            return ResponseEntity.ok().build();
        }

        log.info("Notificación de Mercado Pago recibida para paymentId={}", paymentId);
        cuotaService.procesarNotificacionMercadoPago(paymentId);
        return ResponseEntity.ok().build();
    }
}
