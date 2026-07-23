package com.almoby.ruralcuruzu.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.service.EmailService;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementación de EmailService que, en vez de mandar un correo real,
 * imprime el contenido en la consola. Es la que se usa por defecto
 * (app.email.provider=console, o si no está configurada la variable EMAIL_PROVIDER)
 * para desarrollar y probar el flujo de "olvidé mi contraseña" sin depender
 * de credenciales SMTP. Para enviar correos reales, ver SmtpEmailService.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.email", name = "provider", havingValue = "console", matchIfMissing = true)
public class ConsoleEmailService implements EmailService {

    private final String urlBaseRestablecer;

    public ConsoleEmailService(
            @Value("${app.frontend.reset-password-url:http://localhost:4200/reset-password}") String urlBaseRestablecer) {
        this.urlBaseRestablecer = urlBaseRestablecer;
    }

    @Override
    public void enviarCorreoRecuperacionPassword(String destinatario, String nombre, String tokenPlano) {
        String enlace = urlBaseRestablecer + "?token=" + tokenPlano;

        log.info("""
                ==================== EMAIL (simulado) ====================
                Para: {}
                Asunto: Recuperación de contraseña - Rural Curuzú
                Hola {},
                Recibimos una solicitud para restablecer tu contraseña.
                Ingresá al siguiente enlace (válido por poco tiempo): {}
                Si no fuiste vos, ignorá este mensaje.
                ============================================================""",
                destinatario, nombre, enlace);
    }

    @Override
    public void enviarCorreoPasswordCambiada(String destinatario, String nombre) {
        log.info("""
                ==================== EMAIL (simulado) ====================
                Para: {}
                Asunto: Tu contraseña fue actualizada - Rural Curuzú
                Hola {},
                Te confirmamos que tu contraseña se cambió con éxito.
                Si no fuiste vos, contactá al administrador de inmediato.
                ============================================================""",
                destinatario, nombre);
    }

    @Override
    public void enviarCorreoConfirmacionSolicitudSocio(String destinatario, String nombre, String numeroSolicitud) {
        log.info("""
                ==================== EMAIL (simulado) ====================
                Para: {}
                Asunto: Recibimos tu solicitud - Rural Curuzú
                Hola {},
                Recibimos tu solicitud para ser socio. Tu número de solicitud es {}.
                Te vamos a avisar por este medio cuando sea revisada.
                ============================================================""",
                destinatario, nombre, numeroSolicitud);
    }

    @Override
    public void enviarCorreoRechazoSolicitudSocio(String destinatario, String nombre, String numeroSolicitud, String motivo) {
        log.info("""
                ==================== EMAIL (simulado) ====================
                Para: {}
                Asunto: Tu solicitud fue rechazada - Rural Curuzú
                Hola {},
                Tu solicitud {} fue rechazada. Motivo: {}
                Si creés que fue un error, podés contactarnos para que la revisemos de nuevo.
                ============================================================""",
                destinatario, nombre, numeroSolicitud, motivo);
    }
}
