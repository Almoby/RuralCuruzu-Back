package com.almoby.ruralcuruzu.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.almoby.ruralcuruzu.exception.MercadoPagoIntegracionException;
import com.almoby.ruralcuruzu.service.EstadoPagoMercadoPago;
import com.almoby.ruralcuruzu.service.MercadoPagoService;
import com.almoby.ruralcuruzu.service.PreferenciaMercadoPago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * Integración real con la API REST de Mercado Pago (Checkout Pro). Ver
 * documento 10.4 y {@link com.almoby.ruralcuruzu.enums.MedioPago#LINK_DE_PAGO}.
 *
 * Referencia de la API usada acá (checkout/preferences y v1/payments):
 * https://www.mercadopago.com.ar/developers/es/reference
 *
 * Importante: esto NO puede probarse de punta a punta sin un
 * MERCADOPAGO_ACCESS_TOKEN real de una cuenta de Mercado Pago (de prueba o
 * de producción) y, para el webhook, una URL pública (no localhost) donde
 * Mercado Pago pueda mandar la notificación.
 */
@Slf4j
@Service
public class MercadoPagoServiceImpl implements MercadoPagoService {

    private static final String BASE_URL = "https://api.mercadopago.com";

    private final RestClient restClient;
    private final String notificationUrl;
    private final String backUrlSuccess;
    private final String backUrlFailure;
    private final String backUrlPending;
    private final boolean tokenConfigurado;

    public MercadoPagoServiceImpl(RestClient.Builder restClientBuilder,
                                   @Value("${mercadopago.access-token}") String accessToken,
                                   @Value("${mercadopago.notification-url}") String notificationUrl,
                                   @Value("${mercadopago.back-urls.success}") String backUrlSuccess,
                                   @Value("${mercadopago.back-urls.failure}") String backUrlFailure,
                                   @Value("${mercadopago.back-urls.pending}") String backUrlPending) {
        this.notificationUrl = notificationUrl;
        this.backUrlSuccess = backUrlSuccess;
        this.backUrlFailure = backUrlFailure;
        this.backUrlPending = backUrlPending;
        this.tokenConfigurado = accessToken != null && !accessToken.isBlank();
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();

        if (!tokenConfigurado) {
            log.warn("mercadopago.access-token no está configurado: el link de pago no va a funcionar "
                    + "hasta que se cargue MERCADOPAGO_ACCESS_TOKEN");
        }
    }

    @Override
    public PreferenciaMercadoPago crearPreferencia(String pagoId, String descripcion, BigDecimal importe) {
        exigirTokenConfigurado();

        ItemPreferencia item = new ItemPreferencia(descripcion, 1, "ARS", importe);
        BackUrls backUrls = new BackUrls(backUrlSuccess, backUrlFailure, backUrlPending);
        CrearPreferenciaRequest body =
                new CrearPreferenciaRequest(List.of(item), pagoId, backUrls, "approved", notificationUrl);

        try {
            PreferenciaWire respuesta = restClient.post()
                    .uri("/checkout/preferences")
                    .body(body)
                    .retrieve()
                    .body(PreferenciaWire.class);

            if (respuesta == null || respuesta.id() == null || respuesta.initPoint() == null) {
                throw new MercadoPagoIntegracionException(
                        "Mercado Pago no devolvió una preferencia válida para el pago " + pagoId);
            }

            log.info("Preferencia de Mercado Pago creada para pago id={} preferenceId={}", pagoId, respuesta.id());
            return new PreferenciaMercadoPago(respuesta.id(), respuesta.initPoint());
        } catch (RestClientException ex) {
            throw new MercadoPagoIntegracionException(
                    "Error al crear la preferencia de Mercado Pago para el pago " + pagoId, ex);
        }
    }

    @Override
    public EstadoPagoMercadoPago consultarPago(String mercadoPagoPaymentId) {
        exigirTokenConfigurado();

        try {
            PagoWire respuesta = restClient.get()
                    .uri("/v1/payments/{id}", mercadoPagoPaymentId)
                    .retrieve()
                    .body(PagoWire.class);

            if (respuesta == null) {
                throw new MercadoPagoIntegracionException(
                        "Mercado Pago no devolvió información para el pago " + mercadoPagoPaymentId);
            }

            return new EstadoPagoMercadoPago(String.valueOf(respuesta.id()), respuesta.status(),
                    respuesta.externalReference());
        } catch (RestClientException ex) {
            throw new MercadoPagoIntegracionException(
                    "Error al consultar el pago " + mercadoPagoPaymentId + " en Mercado Pago", ex);
        }
    }

    private void exigirTokenConfigurado() {
        if (!tokenConfigurado) {
            throw new MercadoPagoIntegracionException(
                    "Mercado Pago no está configurado (falta MERCADOPAGO_ACCESS_TOKEN)");
        }
    }

    // --- Formas del JSON de Mercado Pago, solo para uso interno de esta clase. ---

    private record ItemPreferencia(String title, int quantity, String currency_id, BigDecimal unit_price) {
    }

    private record BackUrls(String success, String failure, String pending) {
    }

    private record CrearPreferenciaRequest(List<ItemPreferencia> items, String external_reference,
                                            BackUrls back_urls, String auto_return, String notification_url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PreferenciaWire(String id, @JsonProperty("init_point") String initPoint) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PagoWire(Long id, String status, @JsonProperty("external_reference") String externalReference) {
    }
}
