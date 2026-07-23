package com.almoby.ruralcuruzu.exception;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Se lanza cuando la cuenta tiene demasiadas contraseñas incorrectas seguidas
 * y quedó bloqueada temporalmente (independiente del campo "estado", que es
 * para bloqueos manuales del admin). El mensaje sí confirma que la cuenta
 * existe (a diferencia de CredencialesInvalidasException), pero es un
 * trade-off aceptado: sin esto, un usuario real bloqueado no tendría forma
 * de saber por qué no puede entrar aunque use la contraseña correcta.
 */
public class CuentaBloqueadaTemporalmenteException extends RuntimeException {

    public CuentaBloqueadaTemporalmenteException(Instant bloqueadoHasta) {
        super("Cuenta bloqueada temporalmente por demasiados intentos fallidos. Probá de nuevo en "
                + Math.max(1, ChronoUnit.MINUTES.between(Instant.now(), bloqueadoHasta)) + " minutos");
    }
}
