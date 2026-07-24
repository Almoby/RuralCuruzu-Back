package com.almoby.ruralcuruzu.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Override
    public void enviarCorreoCredencialesSocio(String destinatario, String nombre, String numeroSocio, String passwordTemporal) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("¡Bienvenido a Rural Curuzú! Tus credenciales de acceso");
            helper.setText(cuerpoTextoPlanoCredenciales(nombre, numeroSocio, destinatario, passwordTemporal),
                    cuerpoHtmlCredenciales(nombre, numeroSocio, destinatario, passwordTemporal));

            mailSender.send(mensaje);
            log.info("Correo de credenciales de socio enviado a email={} numeroSocio={}", destinatario, numeroSocio);
        } catch (MessagingException | MailException ex) {
            // A diferencia de los otros correos de aviso, acá si falla el envío el
            // socio se queda sin poder acceder a la plataforma: se relanza como error
            // de infraestructura para que quede visible (500) y se pueda reintentar
            // manualmente en vez de quedar en un estado silencioso a medias.
            log.error("Error enviando correo de credenciales a email={} numeroSocio={}", destinatario, numeroSocio, ex);
            throw new IllegalStateException("No se pudo enviar el correo de credenciales del socio", ex);
        }
    }

    private String cuerpoTextoPlanoCredenciales(String nombre, String numeroSocio, String usuario, String passwordTemporal) {
        return """
                Hola %s,

                ¡Tu solicitud para ser socio de Rural Curuzú fue aprobada!
                Tu número de socio es: %s

                Ya podés ingresar a la plataforma con estas credenciales:
                Usuario: %s
                Contraseña temporal: %s

                Por seguridad, vas a tener que elegir una nueva contraseña la primera vez que ingreses.
                """.formatted(nombre, numeroSocio, usuario, passwordTemporal);
    }

    private String cuerpoHtmlCredenciales(String nombre, String numeroSocio, String usuario, String passwordTemporal) {
        return """
                <p>Hola %s,</p>
                <p>¡Tu solicitud para ser socio de <strong>Rural Curuzú</strong> fue aprobada!</p>
                <p>Tu número de socio es: <strong>%s</strong></p>
                <p>Ya podés ingresar a la plataforma con estas credenciales:</p>
                <p>Usuario: <strong>%s</strong><br>Contraseña temporal: <strong>%s</strong></p>
                <p>Por seguridad, vas a tener que elegir una nueva contraseña la primera vez que ingreses.</p>
                """.formatted(nombre, numeroSocio, usuario, passwordTemporal);
    }

    @Override
    public void enviarCorreoCredencialesComercio(String destinatario, String nombreComercial, String passwordTemporal) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("¡Bienvenido a Rural Curuzú! Tus credenciales de acceso");
            helper.setText(cuerpoTextoPlanoCredencialesComercio(nombreComercial, destinatario, passwordTemporal),
                    cuerpoHtmlCredencialesComercio(nombreComercial, destinatario, passwordTemporal));

            mailSender.send(mensaje);
            log.info("Correo de credenciales de comercio enviado a email={}", destinatario);
        } catch (MessagingException | MailException ex) {
            // Igual que con las credenciales de socio: si esto falla, el comercio se
            // queda sin poder acceder, así que se relanza en vez de tragarse el error.
            log.error("Error enviando correo de credenciales de comercio a email={}", destinatario, ex);
            throw new IllegalStateException("No se pudo enviar el correo de credenciales del comercio", ex);
        }
    }

    private String cuerpoTextoPlanoCredencialesComercio(String nombreComercial, String usuario, String passwordTemporal) {
        return """
                Hola %s,

                Tu comercio fue dado de alta en Rural Curuzú.

                Ya podés ingresar a la plataforma con estas credenciales:
                Usuario: %s
                Contraseña temporal: %s

                Por seguridad, vas a tener que elegir una nueva contraseña la primera vez que ingreses.
                """.formatted(nombreComercial, usuario, passwordTemporal);
    }

    private String cuerpoHtmlCredencialesComercio(String nombreComercial, String usuario, String passwordTemporal) {
        return """
                <p>Hola %s,</p>
                <p>Tu comercio fue dado de alta en <strong>Rural Curuzú</strong>.</p>
                <p>Ya podés ingresar a la plataforma con estas credenciales:</p>
                <p>Usuario: <strong>%s</strong><br>Contraseña temporal: <strong>%s</strong></p>
                <p>Por seguridad, vas a tener que elegir una nueva contraseña la primera vez que ingreses.</p>
                """.formatted(nombreComercial, usuario, passwordTemporal);
    }

    @Override
    public void enviarCorreoCuotaGenerada(String destinatario, String nombre, String periodo,
                                           BigDecimal importe, LocalDate fechaVencimiento) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("Se generó tu cuota de " + periodo + " - Rural Curuzú");
            helper.setText(cuerpoTextoPlanoCuotaGenerada(nombre, periodo, importe, fechaVencimiento),
                    cuerpoHtmlCuotaGenerada(nombre, periodo, importe, fechaVencimiento));

            mailSender.send(mensaje);
            log.info("Correo de cuota generada enviado a email={} periodo={}", destinatario, periodo);
        } catch (MessagingException | MailException ex) {
            // La cuota YA se generó con éxito en la base: que falle este aviso
            // no debe interrumpir la generación del resto de las cuotas.
            log.error("Error enviando correo de cuota generada a email={} periodo={}", destinatario, periodo, ex);
        }
    }

    private String cuerpoTextoPlanoCuotaGenerada(String nombre, String periodo, BigDecimal importe, LocalDate vencimiento) {
        return """
                Hola %s,

                Se generó tu cuota del período %s por $%s.
                Vence el %s.
                """.formatted(nombre, periodo, importe, vencimiento);
    }

    private String cuerpoHtmlCuotaGenerada(String nombre, String periodo, BigDecimal importe, LocalDate vencimiento) {
        return """
                <p>Hola %s,</p>
                <p>Se generó tu cuota del período <strong>%s</strong> por <strong>$%s</strong>.</p>
                <p>Vence el <strong>%s</strong>.</p>
                """.formatted(nombre, periodo, importe, vencimiento);
    }

    @Override
    public void enviarCorreoPagoRegistrado(String destinatario, String nombre, String periodo, BigDecimal importe) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("Confirmamos tu pago - Rural Curuzú");
            helper.setText(cuerpoTextoPlanoPagoRegistrado(nombre, periodo, importe),
                    cuerpoHtmlPagoRegistrado(nombre, periodo, importe));

            mailSender.send(mensaje);
            log.info("Correo de pago registrado enviado a email={} periodo={}", destinatario, periodo);
        } catch (MessagingException | MailException ex) {
            // El pago YA quedó registrado con éxito: que falle este aviso no
            // debe romper la respuesta de la operación.
            log.error("Error enviando correo de pago registrado a email={} periodo={}", destinatario, periodo, ex);
        }
    }

    private String cuerpoTextoPlanoPagoRegistrado(String nombre, String periodo, BigDecimal importe) {
        return """
                Hola %s,

                Confirmamos el pago de tu cuota del período %s por $%s.
                """.formatted(nombre, periodo, importe);
    }

    private String cuerpoHtmlPagoRegistrado(String nombre, String periodo, BigDecimal importe) {
        return """
                <p>Hola %s,</p>
                <p>Confirmamos el pago de tu cuota del período <strong>%s</strong> por <strong>$%s</strong>.</p>
                """.formatted(nombre, periodo, importe);
    }

    @Override
    public void enviarCorreoPagoRechazado(String destinatario, String nombre, String periodo, String motivo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("Tu pago informado fue rechazado - Rural Curuzú");
            helper.setText(cuerpoTextoPlanoPagoRechazado(nombre, periodo, motivo),
                    cuerpoHtmlPagoRechazado(nombre, periodo, motivo));

            mailSender.send(mensaje);
            log.info("Correo de pago rechazado enviado a email={} periodo={}", destinatario, periodo);
        } catch (MessagingException | MailException ex) {
            log.error("Error enviando correo de pago rechazado a email={} periodo={}", destinatario, periodo, ex);
        }
    }

    private String cuerpoTextoPlanoPagoRechazado(String nombre, String periodo, String motivo) {
        return """
                Hola %s,

                El pago que informaste para la cuota del período %s fue rechazado.
                Motivo: %s
                """.formatted(nombre, periodo, motivo);
    }

    private String cuerpoHtmlPagoRechazado(String nombre, String periodo, String motivo) {
        return """
                <p>Hola %s,</p>
                <p>El pago que informaste para la cuota del período <strong>%s</strong> fue rechazado.</p>
                <p>Motivo: %s</p>
                """.formatted(nombre, periodo, motivo);
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
