package com.almoby.ruralcuruzu.enums;

/**
 * Medio de pago de una cuota. Según lo aclarado por el dueño del proyecto:
 * hay dos formas de pagar una cuota. Por transferencia informada por el
 * socio desde el sistema (sube el comprobante y espera que un admin la
 * apruebe), o presencialmente en la oficina rural ("ventanilla"), donde el
 * admin cobra en persona y solo registra el pago sin esperar revisión —
 * ahí puede ser en efectivo, débito/crédito (postnet) o incluso una
 * transferencia hecha en el momento.
 *
 * VENTANILLA queda como opción genérica para cuando el admin registra un
 * pago presencial sin necesidad de precisar si fue efectivo, débito o
 * transferencia.
 */
public enum MedioPago {
    EFECTIVO,
    VENTANILLA,
    TRANSFERENCIA,
    DEBITO
}
