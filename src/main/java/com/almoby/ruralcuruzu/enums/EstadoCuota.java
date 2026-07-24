package com.almoby.ruralcuruzu.enums;

/**
 * Estados de una cuota (documento, sección 10.3).
 * Flujo implementado:
 * - PENDIENTE: recién generada, todavía sin pagar.
 * - EN_REVISION: el socio informó un pago (autoservicio) y está a la espera
 *   de que un admin lo apruebe o rechace. INFORMADA queda reservado para un
 *   futuro paso intermedio (ej. distinguir "recién informado" de "un admin ya
 *   lo está mirando"), pero hoy no se usa: el informe pasa directo a EN_REVISION.
 * - PAGADA: confirmada, ya sea por registro manual del admin (10.4) o por
 *   aprobación de una revisión.
 * - VENCIDA: quedó PENDIENTE después de su fecha de vencimiento (job diario).
 * - RECHAZADA: un admin rechazó el pago informado por el socio.
 * - ANULADA: el admin la anuló (ej. se generó por error); no cuenta como deuda.
 */
public enum EstadoCuota {
    PENDIENTE,
    INFORMADA,
    EN_REVISION,
    PAGADA,
    VENCIDA,
    RECHAZADA,
    ANULADA
}
