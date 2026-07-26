package com.almoby.ruralcuruzu.enums;

/**
 * Estado de un beneficio, independiente de su vigencia por fecha: un
 * beneficio ACTIVO pero con fechaFinVigencia pasada tampoco se muestra a
 * los socios (ver Beneficio.estaVigenteHoy()).
 */
public enum EstadoBeneficio {
    ACTIVO,
    INACTIVO
}
