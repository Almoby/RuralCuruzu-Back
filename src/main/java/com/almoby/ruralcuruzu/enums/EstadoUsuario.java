package com.almoby.ruralcuruzu.enums;

/**
 * Estado de la cuenta de acceso (login) de un usuario.
 * No debe confundirse con el estado administrativo del Socio o del Comercio:
 * este estado solo determina si la cuenta puede o no iniciar sesión.
 */
public enum EstadoUsuario {
    ACTIVO,
    INACTIVO,
    BLOQUEADO,
    PENDIENTE_DE_ACTIVACION,
    DADO_DE_BAJA
}
