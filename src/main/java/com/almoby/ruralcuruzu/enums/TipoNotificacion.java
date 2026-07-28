package com.almoby.ruralcuruzu.enums;

/**
 * Tipo de correo/notificación automática (documento, sección 29.1). Cada
 * valor corresponde a un evento del sistema que dispara un aviso; se usa
 * tanto para el registro de envíos (29.3) como para la campanita in-app,
 * ya que {@link com.almoby.ruralcuruzu.domain.Notificacion} sirve para
 * ambos propósitos.
 */
public enum TipoNotificacion {

    SOLICITUD_RECIBIDA,

    /**
     * Reservado por el documento (29.1), pero no se manda como correo aparte:
     * cuando se aprueba una solicitud, el aviso de aprobación y las
     * credenciales de acceso van en un único correo (ver CREDENCIALES_ACCESO),
     * para no mandarle dos correos seguidos al mismo socio. Mismo criterio
     * que EstadoCuota.INFORMADA: queda documentado en el enum aunque hoy no
     * se use como tipo real de ningún envío.
     */
    SOLICITUD_APROBADA,

    SOLICITUD_RECHAZADA,
    SOLICITUD_OBSERVACION,
    /** Aviso a un admin de que un solicitante respondió una observación. */
    SOLICITUD_RESPUESTA_RECIBIDA,

    /** Cubre tanto al socio (tras aprobarse su solicitud, o alta manual) como al comercio. */
    CREDENCIALES_ACCESO,

    CUOTA_GENERADA,
    CUOTA_PROXIMA_A_VENCER,
    CUOTA_VENCIDA,

    PAGO_INFORMADO,
    PAGO_APROBADO,
    PAGO_RECHAZADO,

    CUENTA_AL_DIA,

    RECUPERACION_PASSWORD,
    PASSWORD_CAMBIADA,

    COMERCIO_ELIMINADO
}
