package com.almoby.ruralcuruzu.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.Notificacion;
import com.almoby.ruralcuruzu.enums.ResultadoNotificacion;
import com.almoby.ruralcuruzu.enums.TipoNotificacion;
import com.almoby.ruralcuruzu.repository.NotificacionRepository;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.service.EmailSender;
import com.almoby.ruralcuruzu.service.EmailService;

import lombok.extern.slf4j.Slf4j;

/**
 * Decorator de EmailSender (documento, sección 29.3: "guardar destinatario,
 * tipo de correo, asunto, fecha, hora, resultado, intentos, error"). Es la
 * única implementación de {@link EmailService} que ve el resto del código
 * (AuthServiceImpl, CuotaServiceImpl, SolicitudSocioServiceImpl, etc. siguen
 * dependiendo de esa interfaz sin cambios): delega el envío real en
 * {@link EmailSender} (Smtp o Console, según la config) y, antes de
 * devolver el control, registra un {@link Notificacion} con el resultado.
 * Ese mismo registro es lo que alimenta la campanita in-app (NotificacionService).
 */
@Slf4j
@Service
public class NotificacionEmailServiceImpl implements EmailService {

    private final EmailSender emailSender;
    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionEmailServiceImpl(EmailSender emailSender, NotificacionRepository notificacionRepository,
                                         UsuarioRepository usuarioRepository) {
        this.emailSender = emailSender;
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void enviarCorreoRecuperacionPassword(String destinatario, String nombre, String tokenPlano) {
        enviarConRegistro(destinatario, nombre, TipoNotificacion.RECUPERACION_PASSWORD,
                "Recuperación de contraseña - Rural Curuzú", "Te enviamos un enlace para restablecer tu contraseña.",
                true, () -> emailSender.enviarCorreoRecuperacionPassword(destinatario, nombre, tokenPlano));
    }

    @Override
    public void enviarCorreoPasswordCambiada(String destinatario, String nombre) {
        enviarConRegistro(destinatario, nombre, TipoNotificacion.PASSWORD_CAMBIADA,
                "Tu contraseña fue actualizada - Rural Curuzú", "Tu contraseña se cambió con éxito.",
                false, () -> emailSender.enviarCorreoPasswordCambiada(destinatario, nombre));
    }

    @Override
    public void enviarCorreoConfirmacionSolicitudSocio(String destinatario, String nombre, String numeroSolicitud) {
        enviarConRegistro(destinatario, nombre, TipoNotificacion.SOLICITUD_RECIBIDA,
                "Recibimos tu solicitud - Rural Curuzú", "Recibimos tu solicitud " + numeroSolicitud + " para ser socio.",
                false, () -> emailSender.enviarCorreoConfirmacionSolicitudSocio(destinatario, nombre, numeroSolicitud));
    }

    @Override
    public void enviarCorreoRechazoSolicitudSocio(String destinatario, String nombre, String numeroSolicitud,
                                                   String motivo) {
        enviarConRegistro(destinatario, nombre, TipoNotificacion.SOLICITUD_RECHAZADA,
                "Tu solicitud fue rechazada - Rural Curuzú", "Tu solicitud " + numeroSolicitud + " fue rechazada.",
                false, () -> emailSender.enviarCorreoRechazoSolicitudSocio(destinatario, nombre, numeroSolicitud, motivo));
    }

    @Override
    public void enviarCorreoObservacionSolicitudSocio(String destinatario, String nombre, String numeroSolicitud,
                                                       String observacion, String enlaceRespuesta) {
        enviarConRegistro(destinatario, nombre, TipoNotificacion.SOLICITUD_OBSERVACION,
                "Novedades sobre tu solicitud - Rural Curuzú",
                "Tu solicitud " + numeroSolicitud + " tiene una observación.",
                false, () -> emailSender.enviarCorreoObservacionSolicitudSocio(
                        destinatario, nombre, numeroSolicitud, observacion, enlaceRespuesta));
    }

    @Override
    public void enviarCorreoRespuestaSolicitudRecibida(String destinatarioAdmin, String numeroSolicitud,
                                                        String nombreSolicitante, boolean tieneArchivos) {
        enviarConRegistro(destinatarioAdmin, null, TipoNotificacion.SOLICITUD_RESPUESTA_RECIBIDA,
                "Nueva respuesta en la solicitud " + numeroSolicitud + " - Rural Curuzú",
                nombreSolicitante + " respondió tu observación en la solicitud " + numeroSolicitud + ".",
                false, () -> emailSender.enviarCorreoRespuestaSolicitudRecibida(
                        destinatarioAdmin, numeroSolicitud, nombreSolicitante, tieneArchivos));
    }

    @Override
    public void enviarCorreoCredencialesSocio(String destinatario, String nombre, String numeroSocio,
                                               String passwordTemporal) {
        enviarConRegistro(destinatario, nombre, TipoNotificacion.CREDENCIALES_ACCESO,
                "¡Bienvenido a Rural Curuzú! Tus credenciales de acceso",
                "Tu solicitud fue aprobada. Tu número de socio es " + numeroSocio + ".",
                true, () -> emailSender.enviarCorreoCredencialesSocio(destinatario, nombre, numeroSocio, passwordTemporal));
    }

    @Override
    public void enviarCorreoCredencialesComercio(String destinatario, String nombreComercial,
                                                  String passwordTemporal) {
        enviarConRegistro(destinatario, nombreComercial, TipoNotificacion.CREDENCIALES_ACCESO,
                "¡Bienvenido a Rural Curuzú! Tus credenciales de acceso", "Tu comercio fue dado de alta.",
                true, () -> emailSender.enviarCorreoCredencialesComercio(destinatario, nombreComercial, passwordTemporal));
    }

    @Override
    public void enviarCorreoCuotaGenerada(String destinatario, String nombre, String periodo,
                                           BigDecimal importe, LocalDate fechaVencimiento) {
        enviarConRegistro(destinatario, nombre, TipoNotificacion.CUOTA_GENERADA,
                "Se generó tu cuota de " + periodo + " - Rural Curuzú",
                "Se generó tu cuota del período " + periodo + " por $" + importe + ".",
                false, () -> emailSender.enviarCorreoCuotaGenerada(destinatario, nombre, periodo, importe, fechaVencimiento));
    }

    @Override
    public void enviarCorreoPagoRegistrado(String destinatario, String nombre, String periodo, BigDecimal importe) {
        enviarConRegistro(destinatario, nombre, TipoNotificacion.PAGO_APROBADO,
                "Confirmamos tu pago - Rural Curuzú",
                "Confirmamos el pago de tu cuota del período " + periodo + " por $" + importe + ".",
                false, () -> emailSender.enviarCorreoPagoRegistrado(destinatario, nombre, periodo, importe));
    }

    @Override
    public void enviarCorreoPagoRechazado(String destinatario, String nombre, String periodo, String motivo) {
        enviarConRegistro(destinatario, nombre, TipoNotificacion.PAGO_RECHAZADO,
                "Tu pago informado fue rechazado - Rural Curuzú",
                "El pago que informaste para la cuota del período " + periodo + " fue rechazado.",
                false, () -> emailSender.enviarCorreoPagoRechazado(destinatario, nombre, periodo, motivo));
    }

    @Override
    public void enviarCorreoComercioEliminado(String destinatario, String nombreComercial, String motivo) {
        enviarConRegistro(destinatario, nombreComercial, TipoNotificacion.COMERCIO_ELIMINADO,
                "Tu comercio fue dado de baja - Rural Curuzú",
                "El comercio " + nombreComercial + " fue eliminado de la plataforma.",
                false, () -> emailSender.enviarCorreoComercioEliminado(destinatario, nombreComercial, motivo));
    }

    @Override
    public void enviarCorreoCuotaProximaAVencer(String destinatario, String nombre, String periodo,
                                                 BigDecimal importe, LocalDate fechaVencimiento, int diasRestantes) {
        enviarConRegistro(destinatario, nombre, TipoNotificacion.CUOTA_PROXIMA_A_VENCER,
                "Tu cuota está por vencer - Rural Curuzú",
                "Tu cuota del período " + periodo + " por $" + importe + " vence el " + fechaVencimiento + ".",
                false, () -> emailSender.enviarCorreoCuotaProximaAVencer(
                        destinatario, nombre, periodo, importe, fechaVencimiento, diasRestantes));
    }

    @Override
    public void enviarCorreoCuotaVencida(String destinatario, String nombre, String periodo,
                                          BigDecimal importe, LocalDate fechaVencimiento) {
        enviarConRegistro(destinatario, nombre, TipoNotificacion.CUOTA_VENCIDA,
                "Tenés una cuota vencida - Rural Curuzú",
                "Tu cuota del período " + periodo + " por $" + importe + " venció el " + fechaVencimiento + ".",
                false, () -> emailSender.enviarCorreoCuotaVencida(destinatario, nombre, periodo, importe, fechaVencimiento));
    }

    @Override
    public void enviarCorreoPagoInformado(String destinatarioAdmin, String numeroSocio, String nombreSocio,
                                           String periodo, BigDecimal importe) {
        enviarConRegistro(destinatarioAdmin, null, TipoNotificacion.PAGO_INFORMADO,
                "Nuevo pago informado para revisar - Rural Curuzú",
                nombreSocio + " (socio " + numeroSocio + ") informó el pago de " + periodo + ".",
                false, () -> emailSender.enviarCorreoPagoInformado(destinatarioAdmin, numeroSocio, nombreSocio, periodo, importe));
    }

    @Override
    public void enviarCorreoCuentaAlDia(String destinatario, String nombre) {
        enviarConRegistro(destinatario, nombre, TipoNotificacion.CUENTA_AL_DIA,
                "Tu cuenta está al día - Rural Curuzú", "No tenés ninguna cuota pendiente.",
                false, () -> emailSender.enviarCorreoCuentaAlDia(destinatario, nombre));
    }

    /**
     * Envuelve un envío real con el registro de la sección 29.3. relanzarEnFallo
     * preserva el mismo contrato que tenían los métodos de EmailService antes de
     * este decorator: algunos (credenciales, recuperación de contraseña) son
     * críticos y deben propagar la falla como error de infraestructura; el resto
     * son avisos best-effort que no deben romper la operación de negocio que los
     * disparó.
     */
    private void enviarConRegistro(String destinatario, String nombreDestinatario, TipoNotificacion tipo,
                                    String asunto, String mensaje, boolean relanzarEnFallo, Runnable envio) {
        try {
            envio.run();
            registrar(destinatario, nombreDestinatario, tipo, asunto, mensaje, ResultadoNotificacion.EXITOSO, null);
        } catch (RuntimeException ex) {
            registrar(destinatario, nombreDestinatario, tipo, asunto, mensaje,
                    ResultadoNotificacion.FALLIDO, ex.getMessage());
            if (relanzarEnFallo) {
                throw ex;
            }
        }
    }

    private void registrar(String destinatario, String nombreDestinatario, TipoNotificacion tipo, String asunto,
                            String mensaje, ResultadoNotificacion resultado, String error) {
        try {
            String emailNormalizado = destinatario == null ? null : destinatario.trim().toLowerCase();
            String destinatarioId = usuarioRepository.findByEmail(emailNormalizado)
                    .map(usuario -> usuario.getId())
                    .orElse(null);

            Notificacion notificacion = Notificacion.builder()
                    .destinatarioId(destinatarioId)
                    .destinatarioEmail(destinatario)
                    .destinatarioNombre(nombreDestinatario)
                    .tipo(tipo)
                    .asunto(asunto)
                    .mensaje(mensaje)
                    .resultado(resultado)
                    .error(error)
                    .fechaEnvio(Instant.now())
                    .build();
            notificacionRepository.save(notificacion);
        } catch (RuntimeException ex) {
            // El registro es auditoría, no una regla de negocio: si falla (ej. Mongo
            // caído), no debe tumbar el envío del correo que ya se intentó/logró.
            log.error("No se pudo registrar la notificación tipo={} destinatario={}", tipo, destinatario, ex);
        }
    }
}
