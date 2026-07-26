package com.almoby.ruralcuruzu.dto.response;

/**
 * Código QR propio de un socio (módulo Beneficios): lo muestra en pantalla,
 * el comercio lo escanea para aplicarle un beneficio.
 */
public record MiQrResponse(

        String codigoQr,
        String numeroSocio,
        String nombre

) {
}
