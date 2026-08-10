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
 *
 * {@code codigoQr} representa exactamente al mismo {@code token} (mismo
 * socio, misma expiración), pero en un formato corto pensado para que el
 * socio lo lea o tipee a mano en el campo "Pegá el código del QR" del
 * comercio, si el escaneo con cámara no anduvo (ver CodigoQrCortoService).
 */
public record MiQrResponse(

        String token,
        String codigoQr,
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
