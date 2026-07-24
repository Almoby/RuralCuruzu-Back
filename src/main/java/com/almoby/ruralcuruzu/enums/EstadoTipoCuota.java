package com.almoby.ruralcuruzu.enums;

/**
 * Estado de un tipo de cuota (documento, sección 10.1). Solo un tipo ACTIVO
 * se tiene en cuenta al generar cuotas mensuales; INACTIVO lo deja disponible
 * para historial sin que se le siga generando cuotas a nadie.
 */
public enum EstadoTipoCuota {
    ACTIVO,
    INACTIVO
}
