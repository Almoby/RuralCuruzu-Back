package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;
import java.time.LocalDate;

import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoQr;

/**
 * "Mi QR" del socio (documento, sección 15). El QR ya no es un código fijo:
 * {@code token} vence en {@code expiraEn} (ver QrTokenService), así que la
 * app del socio debe volver a pedir este endpoint antes de esa fecha para
 * refrescar el QR en pantalla. {@code estado}/{@code mensaje} también se
 * recalculan en cada consulta según la situación actual del socio (cuota,
 * suspensión, baja, bloqueo — ver EstadoQrService).
 */
public record MiQrResponse(

        String token,
        Instant expiraEn,
        String numeroSocio,
        String nombre,
        CategoriaSocio categoria,
        EstadoQr estado,
        String mensaje,
        LocalDate fechaValidez,
        Instant ultimoPago

) {
}
