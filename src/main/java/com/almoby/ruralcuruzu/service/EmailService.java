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
     * Avisa al solicitante que un admin agregó una observación a su solicitud
     * (documento, sección 8.3: "solicitar correcciones"/"solicitar
     * documentación" son casos de uso de esto). Sin este correo, el
     * solicitante no tiene ninguna forma de enterarse de que le piden algo.
     * Incluye un enlace (con un token de un solo uso) para que pueda
     * responder con texto y/o documentación sin necesitar una cuenta.
     */
    void enviarCorreoObservacionSolicitudSocio(String destinatario, String nombre, String numeroSolicitud,
                                                String observacion, String enlaceRespuesta);

    /**
     * Avisa a un admin que un solicitante respondió una observación (con o
     * sin archivos adjuntos), para que sepa que hay algo nuevo para revisar.
     */
    void enviarCorreoRespuestaSolicitudRecibida(String destinatarioAdmin, String numeroSolicitud,
                                                 String nombreSolicitante, boolean tieneArchivos);

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

    /**
     * Avisa al comercio que fue eliminado de la plataforma, con el motivo.
     * Se manda antes de borrar nada (la cuenta de acceso del comercio
     * también se elimina como parte de la baja).
     */
    void enviarCorreoComercioEliminado(String destinatario, String nombreComercial, String motivo);

    /**
     * Recuerda al socio que una cuota esta por vencer (documento, seccion 29.2:
     * "cinco dias antes" y "un dia antes" de recordatorio, y "dia de
     * vencimiento" de aviso reutilizan este mismo correo con distinto
     * diasRestantes).
     */
    void enviarCorreoCuotaProximaAVencer(String destinatario, String nombre, String periodo,
                                          BigDecimal importe, LocalDate fechaVencimiento, int diasRestantes);

    /**
     * Avisa que una cuota quedo vencida y sigue impaga (documento, seccion
     * 29.2: "luego del vencimiento" y el "recordatorio periodico mientras
     * exista deuda" reutilizan este mismo correo).
     */
    void enviarCorreoCuotaVencida(String destinatario, String nombre, String periodo,
                                   BigDecimal importe, LocalDate fechaVencimiento);

    /**
     * Avisa a un admin que un socio informo el pago de una cuota (transferencia)
     * y esta a la espera de revision (documento, seccion 29.1: "pago informado").
     */
    void enviarCorreoPagoInformado(String destinatarioAdmin, String numeroSocio, String nombreSocio,
                                    String periodo, BigDecimal importe);

    /**
     * Felicita al socio cuando, tras un pago, ya no le queda ninguna cuota
     * pendiente, vencida o en revision (documento, seccion 29.1: "cuenta al dia").
     */
    void enviarCorreoCuentaAlDia(String destinatario, String nombre);
}
