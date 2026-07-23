package com.almoby.ruralcuruzu.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación real de EmailService: manda el correo por SMTP (Gmail por
 * defecto, configurable vía spring.mail.*) usando JavaMailSender.
 * Se activa solo con app.email.provider=smtp (variable de entorno EMAIL_PROVIDER=smtp);
 * mientras esa propiedad no esté en "smtp", sigue activa ConsoleEmailService.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.email", name = "provider", havingValue = "smtp")
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final String remitente;
    private final String urlBaseRestablecer;

    public SmtpEmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String remitente,
            @Value("${app.frontend.reset-password-url:http://localhost:4200/reset-password}") String urlBaseRestablecer) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.urlBaseRestablecer = urlBaseRestablecer;
    }

    @Override
    public void enviarCorreoRecuperacionPassword(String destinatario, String nombre, String tokenPlano) {
        String enlace = urlBaseRestablecer + "?token=" + tokenPlano;

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            // El segundo parámetro (true) marca el mensaje como "multipart":
            // hace falta para poder mandar texto plano + HTML como alternativas.
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("Recuperación de contraseña - Rural Curuzú");
            helper.setText(cuerpoTextoPlano(nombre, enlace), cuerpoHtml(nombre, enlace));

            mailSender.send(mensaje);
            log.info("Correo de recuperación enviado a email={}", destinatario);
        } catch (MessagingException | MailException ex) {
            // No relanzamos una excepción de negocio propia: esto es un fallo de
            // infraestructura (SMTP caído, credenciales inválidas, etc.), no una
            // regla de negocio. GlobalExceptionHandler lo toma como error 500
            // y lo loguea completo para poder diagnosticarlo.
            log.error("Error enviando correo de recuperación a email={}", destinatario, ex);
            throw new IllegalStateException("No se pudo enviar el correo de recuperación", ex);
        }
    }

    @Override
    public void enviarCorreoPasswordCambiada(String destinatario, String nombre) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("Tu contraseña fue actualizada - Rural Curuzú");
            helper.setText(cuerpoTextoPlanoConfirmacion(nombre), cuerpoHtmlConfirmacion(nombre));

            mailSender.send(mensaje);
            log.info("Correo de confirmación de cambio de contraseña enviado a email={}", destinatario);
        } catch (MessagingException | MailException ex) {
            // A diferencia del correo de recuperación, acá la contraseña YA se
            // cambió con éxito en la base: que falle este aviso no debe romper
            // la respuesta del reset-password. Se loguea como error para
            // poder detectarlo, pero no se relanza.
            log.error("Error enviando correo de confirmación de cambio de contraseña a email={}", destinatario, ex);
        }
    }

    @Override
    public void enviarCorreoConfirmacionSolicitudSocio(String destinatario, String nombre, String numeroSolicitud) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("Recibimos tu solicitud - Rural Curuzú");
            helper.setText(cuerpoTextoPlanoSolicitud(nombre, numeroSolicitud), cuerpoHtmlSolicitud(nombre, numeroSolicitud));

            mailSender.send(mensaje);
            log.info("Correo de confirmación de solicitud de socio enviado a email={} numeroSolicitud={}",
                    destinatario, numeroSolicitud);
        } catch (MessagingException | MailException ex) {
            // Igual que el correo de confirmación de cambio de contraseña: la solicitud
            // YA se guardó con éxito, que falle este aviso no debe romper la respuesta.
            log.error("Error enviando correo de confirmación de solicitud a email={} numeroSolicitud={}",
                    destinatario, numeroSolicitud, ex);
        }
    }

    @Override
    public void enviarCorreoRechazoSolicitudSocio(String destinatario, String nombre, String numeroSolicitud, String motivo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("Tu solicitud fue rechazada - Rural Curuzú");
            helper.setText(cuerpoTextoPlanoRechazo(nombre, numeroSolicitud, motivo),
                    cuerpoHtmlRechazo(nombre, numeroSolicitud, motivo));

            mailSender.send(mensaje);
            log.info("Correo de rechazo de solicitud de socio enviado a email={} numeroSolicitud={}",
                    destinatario, numeroSolicitud);
        } catch (MessagingException | MailException ex) {
            // Igual que los otros correos de aviso: la solicitud YA se rechazó con
            // éxito, que falle este aviso no debe romper la respuesta.
            log.error("Error enviando correo de rechazo de solicitud a email={} numeroSolicitud={}",
                    destinatario, numeroSolicitud, ex);
        }
    }

    private String cuerpoTextoPlano(String nombre, String enlace) {
        return """
                Hola %s,

                Recibimos una solicitud para restablecer tu contraseña en Rural Curuzú.
                Ingresá al siguiente enlace (válido por poco tiempo) para elegir una nueva:
                %s

                Si no fuiste vos quien lo solicitó, podés ignorar este mensaje.
                """.formatted(nombre, enlace);
    }

    private String cuerpoHtml(String nombre, String enlace) {
        return """
                <p>Hola %s,</p>
                <p>Recibimos una solicitud para restablecer tu contraseña en <strong>Rural Curuzú</strong>.</p>
                <p><a href="%s">Hacé clic acá para elegir una nueva contraseña</a> (el enlace vence en poco tiempo).</p>
                <p>Si no fuiste vos quien lo solicitó, podés ignorar este mensaje.</p>
                """.formatted(nombre, enlace);
    }

    private String cuerpoTextoPlanoConfirmacion(String nombre) {
        return """
                Hola %s,

                Te confirmamos que tu contraseña de Rural Curuzú se actualizó con éxito.
                Si no fuiste vos quien hizo este cambio, contactá al administrador de inmediato.
                """.formatted(nombre);
    }

    private String cuerpoHtmlConfirmacion(String nombre) {
        return """
                <p>Hola %s,</p>
                <p>Te confirmamos que tu contraseña de <strong>Rural Curuzú</strong> se actualizó con éxito.</p>
                <p>Si no fuiste vos quien hizo este cambio, contactá al administrador de inmediato.</p>
                """.formatted(nombre);
    }

    private String cuerpoTextoPlanoSolicitud(String nombre, String numeroSolicitud) {
        return """
                Hola %s,

                Recibimos tu solicitud para ser socio de Rural Curuzú.
                Tu número de solicitud es: %s

                Te vamos a avisar por este mismo medio en cuanto sea revisada.
                """.formatted(nombre, numeroSolicitud);
    }

    private String cuerpoHtmlSolicitud(String nombre, String numeroSolicitud) {
        return """
                <p>Hola %s,</p>
                <p>Recibimos tu solicitud para ser socio de <strong>Rural Curuzú</strong>.</p>
                <p>Tu número de solicitud es: <strong>%s</strong></p>
                <p>Te vamos a avisar por este mismo medio en cuanto sea revisada.</p>
                """.formatted(nombre, numeroSolicitud);
    }

    private String cuerpoTextoPlanoRechazo(String nombre, String numeroSolicitud, String motivo) {
        return """
                Hola %s,

                Tu solicitud %s para ser socio de Rural Curuzú fue rechazada.
                Motivo: %s

                Si creés que fue un error, podés contactarnos para que la revisemos de nuevo.
                """.formatted(nombre, numeroSolicitud, motivo);
    }

    private String cuerpoHtmlRechazo(String nombre, String numeroSolicitud, String motivo) {
        return """
                <p>Hola %s,</p>
                <p>Tu solicitud <strong>%s</strong> para ser socio de <strong>Rural Curuzú</strong> fue rechazada.</p>
                <p>Motivo: %s</p>
                <p>Si creés que fue un error, podés contactarnos para que la revisemos de nuevo.</p>
                """.formatted(nombre, numeroSolicitud, motivo);
    }
}
