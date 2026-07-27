package com.almoby.ruralcuruzu.security.jwt;

import java.time.Instant;

/**
 * Resultado de generar un token de "Mi QR" (documento, sección 15): el valor
 * a codificar en el QR y hasta cuándo es válido, para que la app del socio
 * sepa cuándo pedir uno nuevo.
 */
public record QrTokenGenerado(String token, Instant expiraEn) {
}
