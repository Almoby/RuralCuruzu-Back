package com.almoby.ruralcuruzu.service;

import java.math.BigDecimal;

/**
 * Integración con Mercado Pago Checkout Pro (documento 10.4, canal "link de
 * pago"). Ver {@link com.almoby.ruralcuruzu.enums.MedioPago#LINK_DE_PAGO}.
 */
public interface MercadoPagoService {

    /**
     * Crea una preferencia de pago para un {@link com.almoby.ruralcuruzu.domain.Pago}
     * puntual. {@code pagoId} viaja como {@code external_reference} para poder
     * encontrar el Pago de nuevo cuando llegue la notificación del webhook.
     */
    PreferenciaMercadoPago crearPreferencia(String pagoId, String descripcion, BigDecimal importe);

    /**
     * Reconsulta el estado real de un pago directamente en la API de Mercado
     * Pago (nunca se procesa un webhook confiando solo en su contenido).
     */
    EstadoPagoMercadoPago consultarPago(String mercadoPagoPaymentId);
}
