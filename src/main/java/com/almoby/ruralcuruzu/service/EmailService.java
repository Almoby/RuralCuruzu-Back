package com.almoby.ruralcuruzu.service;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    /**
     * Manda las credenciales de acceso a un Socio recién dado de alta al
     * aprobarse su solicitud (documento, sección 8.4, paso 7): incluye su
     * número de socio y una contraseña temporal que debe cambiar en el
     * primer ingreso.
     */
    void enviarCorreoCredencialesSocio(String destinatario, String nombre, String numeroSocio, String passwordTemporal);

    /**
     * Manda las credenciales de acceso a un Comercio recién dado de alta por
     * el admin (documento, sección 12.3): contraseña temporal que debe
     * cambiar en el primer ingreso.
     */
    void enviarCorreoCredencialesComercio(String destinatario, String nombreComercial, String passwordTemporal);

    /**
     * Avisa a un socio que se generó su cuota del período (documento, sección
     * 10.2, paso 7: "enviar aviso por correo").
     */
    void enviarCorreoCuotaGenerada(String destinatario, String nombre, String periodo,
                                    BigDecimal importe, LocalDate fechaVencimiento);

    /**
     * Confirma que un pago de cuota quedó registrado (documento, sección
     * 10.4: "se envía confirmación por correo"), ya sea por registro manual
     * del admin o por aprobación de un pago informado por el socio.
     */
    void enviarCorreoPagoRegistrado(String destinatario, String nombre, String periodo, BigDecimal importe);

    /** Avisa al socio que un admin rechazó el pago que había informado, con el motivo. */
    void enviarCorreoPagoRechazado(String destinatario, String nombre, String periodo, String motivo);
}
