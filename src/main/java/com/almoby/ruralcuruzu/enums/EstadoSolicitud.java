package com.almoby.ruralcuruzu.enums;

/**
 * Estados posibles de una solicitud de socio (documento, sección 5.5).
 * Las transiciones válidas entre estados están centralizadas en
 * SolicitudSocioServiceImpl, no acá.
 */
public enum EstadoSolicitud {
    PENDIENTE,
    EN_REVISION,
    APROBADA,
    RECHAZADA,
    CANCELADA
}
