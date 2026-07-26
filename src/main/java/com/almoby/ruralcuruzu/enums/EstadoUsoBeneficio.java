package com.almoby.ruralcuruzu.enums;

/**
 * Estado de un registro de uso de beneficio (documento 19.3, "Estado de la
 * operación"). El dueño aclaró que la validación por QR es en el momento
 * (sin pasos de revisión posterior): USADO es el estado normal; ANULADO
 * queda para que un admin corrija un registro cargado por error, igual que
 * se puede anular una cuota.
 */
public enum EstadoUsoBeneficio {
    USADO,
    ANULADO
}
