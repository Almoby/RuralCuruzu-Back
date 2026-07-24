package com.almoby.ruralcuruzu.enums;

/**
 * Estado de membresía de un Socio, independiente del estado de sus cuotas
 * (eso se modela aparte, en el módulo de Cuota/Facturación). Determina si
 * la cuenta de Usuario vinculada puede iniciar sesión.
 */
public enum EstadoSocio {
    ACTIVO,
    INACTIVO,
    DADO_DE_BAJA
}
