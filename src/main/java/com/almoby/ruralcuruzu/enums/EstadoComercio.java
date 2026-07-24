package com.almoby.ruralcuruzu.enums;

/**
 * Estado de un Comercio adherido (documento, sección 12.4). Cuando no está
 * ACTIVO: no puede iniciar sesión, no puede validar códigos QR, y sus
 * promociones no se muestran — pero su historial se conserva siempre
 * (ninguno de estos estados borra nada).
 */
public enum EstadoComercio {
    ACTIVO,
    INACTIVO,
    SUSPENDIDO,
    DADO_DE_BAJA
}
