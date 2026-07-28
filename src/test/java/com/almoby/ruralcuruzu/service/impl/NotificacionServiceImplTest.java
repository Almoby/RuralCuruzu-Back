package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.almoby.ruralcuruzu.domain.Notificacion;
import com.almoby.ruralcuruzu.dto.response.ContadorNoLeidasResponse;
import com.almoby.ruralcuruzu.dto.response.NotificacionResponse;
import com.almoby.ruralcuruzu.enums.ResultadoNotificacion;
import com.almoby.ruralcuruzu.enums.TipoNotificacion;
import com.almoby.ruralcuruzu.exception.NotificacionNoEncontradaException;
import com.almoby.ruralcuruzu.repository.NotificacionRepository;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceImplTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    private NotificacionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificacionServiceImpl(notificacionRepository);
    }

    private Notificacion notificacion(String id, String destinatarioId, boolean leida) {
        return Notificacion.builder()
                .id(id)
                .destinatarioId(destinatarioId)
                .destinatarioEmail("juan@example.com")
                .tipo(TipoNotificacion.CUOTA_GENERADA)
                .asunto("Asunto")
                .mensaje("Mensaje")
                .resultado(ResultadoNotificacion.EXITOSO)
                .leida(leida)
                .build();
    }

    @Test
    void listarPropias_devuelveSoloLasDelUsuario() {
        when(notificacionRepository.findByDestinatarioIdOrderByFechaEnvioDesc("usuario-1"))
                .thenReturn(List.of(notificacion("n-1", "usuario-1", false)));

        List<NotificacionResponse> resultado = service.listarPropias("usuario-1");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo("n-1");
    }

    @Test
    void contarNoLeidas_delegaEnElRepositorio() {
        when(notificacionRepository.countByDestinatarioIdAndLeidaFalse("usuario-1")).thenReturn(3L);

        ContadorNoLeidasResponse resultado = service.contarNoLeidas("usuario-1");

        assertThat(resultado.cantidad()).isEqualTo(3L);
    }

    @Test
    void marcarLeida_deLaPropia_laMarcaYGuardaFechaDeLectura() {
        Notificacion notificacion = notificacion("n-1", "usuario-1", false);
        when(notificacionRepository.findById("n-1")).thenReturn(Optional.of(notificacion));

        service.marcarLeida("n-1", "usuario-1");

        assertThat(notificacion.isLeida()).isTrue();
        assertThat(notificacion.getFechaLectura()).isNotNull();
        verify(notificacionRepository).save(notificacion);
    }

    @Test
    void marcarLeida_yaLeida_noVuelveAGuardar() {
        Notificacion notificacion = notificacion("n-1", "usuario-1", true);
        when(notificacionRepository.findById("n-1")).thenReturn(Optional.of(notificacion));

        service.marcarLeida("n-1", "usuario-1");

        verify(notificacionRepository, never()).save(notificacion);
    }

    @Test
    void marcarLeida_deOtroUsuario_lanzaNoEncontrada() {
        Notificacion notificacion = notificacion("n-1", "usuario-1", false);
        when(notificacionRepository.findById("n-1")).thenReturn(Optional.of(notificacion));

        assertThatThrownBy(() -> service.marcarLeida("n-1", "usuario-ajeno"))
                .isInstanceOf(NotificacionNoEncontradaException.class);
        verify(notificacionRepository, never()).save(notificacion);
    }

    @Test
    void marcarLeida_inexistente_lanzaNoEncontrada() {
        when(notificacionRepository.findById("n-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarLeida("n-x", "usuario-1"))
                .isInstanceOf(NotificacionNoEncontradaException.class);
    }
}
