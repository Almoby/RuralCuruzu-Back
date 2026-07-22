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
            @Value("${app.frontend.reset-password-url:http://localhost:4200/restablecer-password}") String urlBaseRestablecer) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.urlBaseRestablecer = urlBaseRestablecer;
    }

    @Override
    public void enviarCorreoRecuperacionPassword(String destinatario, String nombre, String tokenPlano) {
        String enlace = urlBaseRestablecer + "?token=" + tokenPlano;

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");

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
}
