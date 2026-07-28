package com.almoby.ruralcuruzu.service;

/**
 * Resultado de crear una preferencia de pago en Mercado Pago (Checkout Pro).
 * {@code initPoint} es la URL de checkout a la que hay que mandar al socio
 * para que pague; {@code preferenceId} se guarda en {@code Pago.mercadoPagoPreferenceId}
 * para poder correlacionar la notificación del webhook más adelante.
 */
public record PreferenciaMercadoPago(String preferenceId, String initPoint) {
}
