package com.almoby.ruralcuruzu.service;

/**
 * Estado real de un pago consultado directamente contra la API de Mercado
 * Pago (nunca se confía en el contenido del webhook por sí solo: la
 * notificación solo dice "revisá este id", y siempre hay que reconsultarlo).
 *
 * {@code externalReference} es el id de nuestro {@link com.almoby.ruralcuruzu.domain.Pago}
 * que mandamos como {@code external_reference} al crear la preferencia, así
 * volvemos a encontrar el Pago correspondiente.
 *
 * {@code status} viaja tal cual lo devuelve Mercado Pago ("approved",
 * "rejected", "cancelled", "pending", "in_process", etc.).
 */
public record EstadoPagoMercadoPago(String mercadoPagoPaymentId, String status, String externalReference) {

    public boolean aprobado() {
        return "approved".equalsIgnoreCase(status);
    }

    public boolean rechazado() {
        return "rejected".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status);
    }
}
