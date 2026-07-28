package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.almoby.ruralcuruzu.domain.Notificacion;
import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.enums.ResultadoNotificacion;
import com.almoby.ruralcuruzu.enums.TipoNotificacion;
import com.almoby.ruralcuruzu.repository.NotificacionRepository;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.service.EmailSender;

/**
 * El decorator es el corazón del registro de la sección 29.3: por cada envío
 * debe quedar un Notificacion con el resultado correcto, y tiene que
 * preservar el mismo contrato de antes (relanzar o no la excepción) para no
 * romper a quien lo llama.
 */
@ExtendWith(MockitoExtension.class)
class NotificacionEmailServiceImplTest {

    @Mock
    private EmailSender emailSender;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private NotificacionEmailServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificacionEmailServiceImpl(emailSender, notificacionRepository, usuarioRepository);
    }

    @Test
    void enviarCorreoCuotaGenerada_exitoso_registraNotificacionExitosaConDestinatarioResuelto() {
        Usuario usuario = Usuario.builder().id("usuario-1").email("juan@example.com").build();
        when(usuarioRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(usuario));

        service.enviarCorreoCuotaGenerada("juan@example.com", "Juan", "2026-07",
                new java.math.BigDecimal("15000.00"), java.time.LocalDate.of(2026, 7, 10));

        verify(emailSender).enviarCorreoCuotaGenerada("juan@example.com", "Juan", "2026-07",
                new java.math.BigDecimal("15000.00"), java.time.LocalDate.of(2026, 7, 10));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion notificacion = captor.getValue();
        assertThat(notificacion.getDestinatarioId()).isEqualTo("usuario-1");
        assertThat(notificacion.getDestinatarioEmail()).isEqualTo("juan@example.com");
        assertThat(notificacion.getTipo()).isEqualTo(TipoNotificacion.CUOTA_GENERADA);
        assertThat(notificacion.getResultado()).isEqualTo(ResultadoNotificacion.EXITOSO);
        assertThat(notificacion.getError()).isNull();
    }

    @Test
    void enviarCorreoCuotaGenerada_conFallaDeEnvio_registraFallidoYNoRelanza() {
        doThrow(new IllegalStateException("SMTP caído"))
                .when(emailSender).enviarCorreoCuotaGenerada(any(), any(), any(), any(), any());

        // No debe lanzar: es un aviso best-effort (igual que antes de este decorator).
        service.enviarCorreoCuotaGenerada("juan@example.com", "Juan", "2026-07",
                new java.math.BigDecimal("15000.00"), java.time.LocalDate.of(2026, 7, 10));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getResultado()).isEqualTo(ResultadoNotificacion.FALLIDO);
        assertThat(captor.getValue().getError()).isEqualTo("SMTP caído");
    }

    @Test
    void enviarCorreoRecuperacionPassword_conFallaDeEnvio_registraFallidoYRelanza() {
        doThrow(new IllegalStateException("SMTP caído"))
                .when(emailSender).enviarCorreoRecuperacionPassword(any(), any(), any());

        // A diferencia del aviso de cuota generada, este sí debe seguir relanzando
        // (correo crítico: el usuario se queda sin poder resetear la contraseña).
        assertThatThrownBy(() -> service.enviarCorreoRecuperacionPassword("juan@example.com", "Juan", "token-plano"))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getResultado()).isEqualTo(ResultadoNotificacion.FALLIDO);
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoNotificacion.RECUPERACION_PASSWORD);
    }

    @Test
    void enviarCorreo_destinatarioSinUsuarioAsociado_registraConDestinatarioIdNulo() {
        when(usuarioRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());

        service.enviarCorreoPagoInformado("admin@example.com", "SOC-000001", "Juan Pérez", "2026-07",
                new java.math.BigDecimal("15000.00"));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getDestinatarioId()).isNull();
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoNotificacion.PAGO_INFORMADO);
    }

    @Test
    void registrar_conMongoCaido_noRompeElEnvioQueYaSeHizo() {
        when(usuarioRepository.findByEmail(any())).thenThrow(new IllegalStateException("Mongo caído"));

        // El correo se manda igual (ya se ejecutó el Runnable); solo falla el registro,
        // que es auditoría, no una regla de negocio.
        service.enviarCorreoCuotaGenerada("juan@example.com", "Juan", "2026-07",
                new java.math.BigDecimal("15000.00"), java.time.LocalDate.of(2026, 7, 10));

        verify(emailSender).enviarCorreoCuotaGenerada("juan@example.com", "Juan", "2026-07",
                new java.math.BigDecimal("15000.00"), java.time.LocalDate.of(2026, 7, 10));
    }
}
