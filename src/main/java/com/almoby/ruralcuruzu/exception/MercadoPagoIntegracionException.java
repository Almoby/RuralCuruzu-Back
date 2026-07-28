package com.almoby.ruralcuruzu.exception;

/**
 * Falla al hablar con la API de Mercado Pago (token no configurado, timeout,
 * respuesta de error, etc.). Se distingue de los demás errores de negocio
 * porque el problema no es de quien usa nuestra API sino del servicio externo,
 * así que se traduce a 502 (Bad Gateway) en vez de 400/404.
 */
public class MercadoPagoIntegracionException extends RuntimeException {

    public MercadoPagoIntegracionException(String mensaje) {
        super(mensaje);
    }

    public MercadoPagoIntegracionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
