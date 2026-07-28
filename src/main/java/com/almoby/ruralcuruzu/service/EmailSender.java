package com.almoby.ruralcuruzu.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * El envío de correo en sí (SMTP real, o el mock de consola), sin ningún
 * registro ni lógica de negocio alrededor. A propósito NO es la misma
 * interfaz que {@link EmailService} (aunque tiene los mismos métodos): así
 * {@link com.almoby.ruralcuruzu.service.impl.NotificacionEmailServiceImpl}
 * puede ser la única implementación de {@code EmailService} que ve el resto
 * del código (decorator que agrega el registro de la sección 29.3), delegando
 * en esta interfaz para el envío real, sin que Spring tenga ambigüedad entre
 * dos beans de un mismo tipo al inyectar {@code EmailService} en cualquier
 * otro service.
 */
public interface EmailSender {

    void enviarCorreoRecuperacionPassword(String destinatario, String nombre, String tokenPlano);

    void enviarCorreoPasswordCambiada(String destinatario, String nombre);

    void enviarCorreoConfirmacionSolicitudSocio(String destinatario, String nombre, String numeroSolicitud);

    void enviarCorreoRechazoSolicitudSocio(String destinatario, String nombre, String numeroSolicitud, String motivo);

    void enviarCorreoObservacionSolicitudSocio(String destinatario, String nombre, String numeroSolicitud,
                                                String observacion, String enlaceRespuesta);

    void enviarCorreoRespuestaSolicitudRecibida(String destinatarioAdmin, String numeroSolicitud,
                                                 String nombreSolicitante, boolean tieneArchivos);

    void enviarCorreoCredencialesSocio(String destinatario, String nombre, String numeroSocio, String passwordTemporal);

    void enviarCorreoCredencialesComercio(String destinatario, String nombreComercial, String passwordTemporal);

    void enviarCorreoCuotaGenerada(String destinatario, String nombre, String periodo,
                                    BigDecimal importe, LocalDate fechaVencimiento);

    void enviarCorreoPagoRegistrado(String destinatario, String nombre, String periodo, BigDecimal importe);

    void enviarCorreoPagoRechazado(String destinatario, String nombre, String periodo, String motivo);

    void enviarCorreoComercioEliminado(String destinatario, String nombreComercial, String motivo);

    void enviarCorreoCuotaProximaAVencer(String destinatario, String nombre, String periodo,
                                          BigDecimal importe, LocalDate fechaVencimiento, int diasRestantes);

    void enviarCorreoCuotaVencida(String destinatario, String nombre, String periodo,
                                   BigDecimal importe, LocalDate fechaVencimiento);

    void enviarCorreoPagoInformado(String destinatarioAdmin, String numeroSocio, String nombreSocio,
                                    String periodo, BigDecimal importe);

    void enviarCorreoCuentaAlDia(String destinatario, String nombre);
}
