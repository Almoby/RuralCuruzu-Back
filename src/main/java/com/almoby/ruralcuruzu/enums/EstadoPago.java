package com.almoby.ruralcuruzu.enums;

/**
 * Estado de un intento de pago (RN-17: el pago es su propia entidad, no un
 * dato embebido en la cuota). Una misma {@code Cuota} puede tener varios
 * {@code Pago} a lo largo del tiempo (ej. un intento por transferencia
 * rechazado y un segundo intento aprobado): cada uno queda como registro
 * histórico propio, ninguno se sobrescribe ni se borra.
 * - EN_REVISION: recién informado por el socio (transferencia) o con un link
 *   de pago generado y todavía sin confirmar; a la espera de resolución.
 * - APROBADO: confirmado, ya sea por un admin (transferencia informada,
 *   registro manual de ventanilla) o por la pasarela de pago (link de pago).
 * - RECHAZADO: un admin rechazó el pago informado, o la pasarela lo rechazó.
 */
public enum EstadoPago {
    EN_REVISION,
    APROBADO,
    RECHAZADO
}
