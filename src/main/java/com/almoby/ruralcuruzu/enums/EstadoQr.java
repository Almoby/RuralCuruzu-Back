package com.almoby.ruralcuruzu.enums;

/**
 * Estado vigente del QR de un socio (documento, sección 15.3). Se recalcula
 * en el momento, tanto para "Mi QR" (sección 15.4) como para el canje del
 * comercio (sección 15.6): el código en sí no cambia, pero su validez
 * depende del estado actual del socio, de su cuenta de acceso y de sus
 * cuotas. Ver EstadoQrService para el orden de prioridad entre estos casos.
 */
public enum EstadoQr {
    ACTIVO,
    INACTIVO_POR_DEUDA,
    INACTIVO_POR_SUSPENSION,
    VENCIDO,
    BLOQUEADO
}
