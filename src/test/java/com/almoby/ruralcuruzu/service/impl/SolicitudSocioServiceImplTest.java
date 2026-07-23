package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.almoby.ruralcuruzu.constantes.SolicitudSocioConstantes;
import com.almoby.ruralcuruzu.domain.SolicitudSocio;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoSolicitud;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoSolicitudRequest;
import com.almoby.ruralcuruzu.dto.request.DatosPersonaFisicaRequest;
import com.almoby.ruralcuruzu.dto.request.DatosPersonaJuridicaRequest;
import com.almoby.ruralcuruzu.dto.request.SolicitudSocioRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoSolicitudResponse;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioResponse;
import com.almoby.ruralcuruzu.exception.DocumentoYaRegistradoException;
import com.almoby.ruralcuruzu.exception.EmailYaRegistradoException;
import com.almoby.ruralcuruzu.exception.SolicitudNoEncontradaException;
import com.almoby.ruralcuruzu.exception.TransicionEstadoInvalidaException;
import com.almoby.ruralcuruzu.repository.SolicitudSocioRepository;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.service.EmailService;
import com.almoby.ruralcuruzu.service.SecuenciaService;

/**
 * Tests unitarios de la lógica de negocio de SolicitudSocioServiceImpl.
 * Todas las dependencias van mockeadas (sin Spring ni Mongo reales).
 */
@ExtendWith(MockitoExtension.class)
class SolicitudSocioServiceImplTest {

    @Mock
    private SolicitudSocioRepository solicitudSocioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SecuenciaService secuenciaService;
    @Mock
    private EmailService emailService;

    private SolicitudSocioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SolicitudSocioServiceImpl(solicitudSocioRepository, usuarioRepository, secuenciaService, emailService);
    }

    private DatosPersonaFisicaRequest datosFisicaValidos() {
        return new DatosPersonaFisicaRequest(
                "Juan Carlos", "García", "28345678", LocalDate.of(1985, 4, 12),
                "20-28345678-2", "Calle 123", "Depto B", "+54 9 3777123456",
                "juan.garcia@example.com", "Comerciante", null);
    }

    private DatosPersonaJuridicaRequest datosJuridicaValidos() {
        return new DatosPersonaJuridicaRequest(
                "Agropecuaria Curuzú S.A.", "30-71234567-8", "Ruta 123", null,
                "+54 9 3777123456", "contacto@agropecuaria.com", "Agropecuaria Curuzú",
                "María Fernández", "30123456");
    }

    @Test
    void crear_conPersonaFisica_guardaLaSolicitudPendienteYMandaConfirmacion() {
        SolicitudSocioRequest request = new SolicitudSocioRequest(
                CategoriaSocio.ACTIVO, TipoPersona.FISICA, datosFisicaValidos(), null, true);

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(solicitudSocioRepository.existsByEmailIgnoreCaseAndEstadoIn(anyString(), any())).thenReturn(false);
        when(solicitudSocioRepository.existsByDocumentosInAndEstadoIn(any(), any())).thenReturn(false);
        when(secuenciaService.siguienteValor(SolicitudSocioConstantes.NOMBRE_SECUENCIA_NUMERO_SOLICITUD)).thenReturn(123L);

        SolicitudSocioResponse response = service.crearSolicitudSocio(request);

        assertThat(response.numeroSolicitud()).isEqualTo("SOL-000123");
        assertThat(response.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(response.historial()).hasSize(1);

        verify(solicitudSocioRepository).save(any(SolicitudSocio.class));
        verify(emailService).enviarCorreoConfirmacionSolicitudSocio(
                eq("juan.garcia@example.com"), anyString(), eq("SOL-000123"));
    }

    @Test
    void crear_conPersonaJuridica_guardaLaSolicitudPendiente() {
        SolicitudSocioRequest request = new SolicitudSocioRequest(
                CategoriaSocio.ADHERENTE, TipoPersona.JURIDICA, null, datosJuridicaValidos(), true);

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(solicitudSocioRepository.existsByEmailIgnoreCaseAndEstadoIn(anyString(), any())).thenReturn(false);
        when(solicitudSocioRepository.existsByDocumentosInAndEstadoIn(any(), any())).thenReturn(false);
        when(secuenciaService.siguienteValor(SolicitudSocioConstantes.NOMBRE_SECUENCIA_NUMERO_SOLICITUD)).thenReturn(7L);

        SolicitudSocioResponse response = service.crearSolicitudSocio(request);

        assertThat(response.numeroSolicitud()).isEqualTo("SOL-000007");
        assertThat(response.tipoPersona()).isEqualTo(TipoPersona.JURIDICA);
        assertThat(response.datosPersonaJuridica().getRazonSocial()).isEqualTo("Agropecuaria Curuzú S.A.");
    }

    @Test
    void crear_conEmailYaRegistradoComoUsuario_lanzaExcepcion() {
        SolicitudSocioRequest request = new SolicitudSocioRequest(
                CategoriaSocio.ACTIVO, TipoPersona.FISICA, datosFisicaValidos(), null, true);

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.crearSolicitudSocio(request)).isInstanceOf(EmailYaRegistradoException.class);
        verify(solicitudSocioRepository, never()).save(any());
    }

    @Test
    void crear_conDocumentoYaRegistradoEnOtraSolicitud_lanzaExcepcion() {
        SolicitudSocioRequest request = new SolicitudSocioRequest(
                CategoriaSocio.ACTIVO, TipoPersona.FISICA, datosFisicaValidos(), null, true);

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(solicitudSocioRepository.existsByEmailIgnoreCaseAndEstadoIn(anyString(), any())).thenReturn(false);
        when(solicitudSocioRepository.existsByDocumentosInAndEstadoIn(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.crearSolicitudSocio(request)).isInstanceOf(DocumentoYaRegistradoException.class);
        verify(solicitudSocioRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_deEnRevisionAAprobada_esUnaTransicionValida() {
        SolicitudSocio solicitud = solicitudPendiente();
        solicitud.setEstado(EstadoSolicitud.EN_REVISION);
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));

        CambiarEstadoSolicitudRequest request = new CambiarEstadoSolicitudRequest(
                EstadoSolicitud.APROBADA, "Documentación OK", null);

        CambiarEstadoSolicitudResponse response = service.cambiarEstadoSolicitudSocio("SOL-000001", request, "admin-1", "Admin Uno");

        assertThat(response.estado()).isEqualTo(EstadoSolicitud.APROBADA);
        assertThat(response.numeroSolicitud()).isEqualTo("SOL-000001");
        verify(solicitudSocioRepository).save(solicitud);
    }

    @Test
    void cambiarEstado_deAprobadaDirectoARechazada_esUnaTransicionInvalida() {
        SolicitudSocio solicitud = solicitudPendiente();
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));

        CambiarEstadoSolicitudRequest request = new CambiarEstadoSolicitudRequest(
                EstadoSolicitud.RECHAZADA, null, "motivo");

        assertThatThrownBy(() -> service.cambiarEstadoSolicitudSocio("SOL-000001", request, "admin-1", "Admin Uno"))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void cambiarEstado_deRechazadaAEnRevision_reabreLaSolicitud() {
        SolicitudSocio solicitud = solicitudPendiente();
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));

        CambiarEstadoSolicitudRequest request = new CambiarEstadoSolicitudRequest(
                EstadoSolicitud.EN_REVISION, "Se reabre a pedido del socio", null);

        CambiarEstadoSolicitudResponse response = service.cambiarEstadoSolicitudSocio("SOL-000001", request, "admin-1", "Admin Uno");

        assertThat(response.estado()).isEqualTo(EstadoSolicitud.EN_REVISION);
        verify(solicitudSocioRepository).save(solicitud);
    }

    @Test
    void cambiarEstado_deAprobadaAEnRevision_reabreLaSolicitud() {
        SolicitudSocio solicitud = solicitudPendiente();
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));

        CambiarEstadoSolicitudRequest request = new CambiarEstadoSolicitudRequest(
                EstadoSolicitud.EN_REVISION, null, null);

        CambiarEstadoSolicitudResponse response = service.cambiarEstadoSolicitudSocio("SOL-000001", request, "admin-1", "Admin Uno");

        assertThat(response.estado()).isEqualTo(EstadoSolicitud.EN_REVISION);
    }

    @Test
    void cambiarEstado_aRechazadaSinMotivo_esInvalido() {
        SolicitudSocio solicitud = solicitudPendiente();
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));

        CambiarEstadoSolicitudRequest request = new CambiarEstadoSolicitudRequest(
                EstadoSolicitud.RECHAZADA, "Observación", null);

        assertThatThrownBy(() -> service.cambiarEstadoSolicitudSocio("SOL-000001", request, "admin-1", "Admin Uno"))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
        verify(solicitudSocioRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_aRechazadaConMotivo_esValido() {
        SolicitudSocio solicitud = solicitudPendiente();
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));

        CambiarEstadoSolicitudRequest request = new CambiarEstadoSolicitudRequest(
                EstadoSolicitud.RECHAZADA, "Observación", "El CUIT no coincide");

        CambiarEstadoSolicitudResponse response = service.cambiarEstadoSolicitudSocio("SOL-000001", request, "admin-1", "Admin Uno");

        assertThat(response.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(response.mensaje()).isEqualTo("Solicitud rechazada correctamente");
        // El historial no viaja en esta respuesta acotada: se verifica sobre la
        // solicitud mutada (mismo objeto que "guardó" el repositorio mockeado).
        assertThat(solicitud.getHistorial()).anyMatch(h -> "El CUIT no coincide".equals(h.getMotivo()));
        verify(emailService).enviarCorreoRechazoSolicitudSocio(
                eq("juan.garcia@example.com"), anyString(), eq("SOL-000001"), eq("El CUIT no coincide"));
    }

    @Test
    void cambiarEstado_solicitudInexistente_lanzaExcepcion() {
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-999999")).thenReturn(Optional.empty());

        CambiarEstadoSolicitudRequest request = new CambiarEstadoSolicitudRequest(EstadoSolicitud.EN_REVISION, null, null);

        assertThatThrownBy(() -> service.cambiarEstadoSolicitudSocio("SOL-999999", request, "admin-1", "Admin Uno"))
                .isInstanceOf(SolicitudNoEncontradaException.class);
    }

    @Test
    void obtenerPorNumero_inexistente_lanzaExcepcion() {
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerSolicitudSocioPorNumero("SOL-999999"))
                .isInstanceOf(SolicitudNoEncontradaException.class);
    }

    @Test
    void listar_sinFiltroDeEstado_usaFindAll() {
        when(solicitudSocioRepository.findAll()).thenReturn(java.util.List.of(solicitudPendiente()));

        java.util.List<?> resultado = service.listarSolicitudesSocio(null);

        assertThat(resultado).hasSize(1);
        verify(solicitudSocioRepository, never()).findByEstado(any());
    }

    @Test
    void listar_conFiltroDeEstado_usaFindByEstado() {
        when(solicitudSocioRepository.findByEstado(EstadoSolicitud.PENDIENTE))
                .thenReturn(java.util.List.of(solicitudPendiente()));

        java.util.List<?> resultado = service.listarSolicitudesSocio(EstadoSolicitud.PENDIENTE);

        assertThat(resultado).hasSize(1);
        verify(solicitudSocioRepository, never()).findAll();
    }

    private SolicitudSocio solicitudPendiente() {
        SolicitudSocio solicitud = SolicitudSocio.builder()
                .id("id-1")
                .numeroSolicitud("SOL-000001")
                .categoriaSolicitada(CategoriaSocio.ACTIVO)
                .tipoPersona(TipoPersona.FISICA)
                .email("juan.garcia@example.com")
                .documentos(java.util.List.of("28345678", "20283456782"))
                .aceptaTerminosYCondiciones(true)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();
        solicitud.setDatosPersonaFisica(new com.almoby.ruralcuruzu.domain.DatosPersonaFisica(
                "Juan Carlos", "García", "28345678", LocalDate.of(1985, 4, 12), "20-28345678-2",
                "Calle 123", "Depto B", "+54 9 3777123456", "juan.garcia@example.com", "Comerciante", null));
        return solicitud;
    }
}
