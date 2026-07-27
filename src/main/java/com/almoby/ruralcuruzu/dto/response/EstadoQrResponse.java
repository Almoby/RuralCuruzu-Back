package com.almoby.ruralcuruzu.dto.response;

import java.time.Instant;
import java.time.LocalDate;

import com.almoby.ruralcuruzu.enums.EstadoQr;

/**
 * Estado vigente del QR de un socio, calculado en el momento (documento,
 * secciones 15.2 y 15.3). {@code fechaValidez} es la fecha de vencimiento de
 * su cuota más reciente (equivalente a "próximo vencimiento"); {@code
 * ultimoPago} es la fecha del último pago acreditado, si tiene alguno.
 */
public record EstadoQrResponse(

        EstadoQr estado,
        String mensaje,
        LocalDate fechaValidez,
        Instant ultimoPago

) {
}
