package com.almoby.ruralcuruzu.service;

/**
 * Abstracción del envío de correos. Hoy solo hay una implementación que
 * loguea el contenido en consola (no hay proveedor SMTP configurado todavía),
 * pero el resto del código depende de esta interfaz, no de la implementación:
 * el día de mañana se agrega una implementación real (SMTP, SES, SendGrid, etc.)
 * sin tocar ni AuthService ni ningún controller.
 */
public interface EmailService {

    /**
     * Envía el correo con el enlace para restablecer la contraseña.
     *
     * @param destinatario email del usuario
     * @param nombre       nombre para personalizar el saludo
     * @param tokenPlano   token en texto plano (sin hashear) que va en el link
     */
    void enviarCorreoRecuperacionPassword(String destinatario, String nombre, String tokenPlano);

    /**
     * Notifica que la contraseña se cambió con éxito (vía reset-password).
     * Es un aviso de seguridad: si alguien restablece la contraseña sin ser
     * el dueño real de la cuenta, el dueño se entera igual por este correo.
     */
    void enviarCorreoPasswordCambiada(String destinatario, String nombre);

    /**
     * Confirma al solicitante que su solicitud de socio fue recibida
     * (documento 5.4: "se envía una confirmación por correo").
     */
    void enviarCorreoConfirmacionSolicitudSocio(String destinatario, String nombre, String numeroSolicitud);

    /**
     * Avisa al solicitante que su solicitud fue rechazada, con el motivo
     * (documento, sección de Rechazo: "se envía un correo al solicitante").
     */
    void enviarCorreoRechazoSolicitudSocio(String destinatario, String nombre, String numeroSolicitud, String motivo);
}
