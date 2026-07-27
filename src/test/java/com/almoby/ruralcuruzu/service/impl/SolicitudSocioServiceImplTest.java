package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.almoby.ruralcuruzu.constantes.SolicitudSocioConstantes;
import com.almoby.ruralcuruzu.domain.CambioEstadoSolicitud;
import com.almoby.ruralcuruzu.domain.SolicitudSocio;
import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoSolicitud;
import com.almoby.ruralcuruzu.enums.Rol;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoSolicitudRequest;
import com.almoby.ruralcuruzu.dto.request.SolicitudSocioRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoSolicitudResponse;
import com.almoby.ruralcuruzu.dto.response.ObservacionAgregadaResponse;
import com.almoby.ruralcuruzu.dto.response.ObservacionPendienteResponse;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioCreadaResponse;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioResponse;
import com.almoby.ruralcuruzu.exception.DocumentoYaRegistradoException;
import com.almoby.ruralcuruzu.exception.EmailYaRegistradoException;
import com.almoby.ruralcuruzu.exception.SolicitudNoEncontradaException;
import com.almoby.ruralcuruzu.exception.TokenRespuestaSolicitudInvalidoException;
import com.almoby.ruralcuruzu.exception.TransicionEstadoInvalidaException;
import com.almoby.ruralcuruzu.repository.SolicitudSocioRepository;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.service.AlmacenamientoArchivosService;
import com.almoby.ruralcuruzu.service.EmailService;
import com.almoby.ruralcuruzu.service.SecuenciaService;
import com.almoby.ruralcuruzu.service.SocioService;
import com.almoby.ruralcuruzu.service.TokenRespuestaSolicitudService;

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
    @Mock
    private SocioService socioService;
    @Mock
    private TokenRespuestaSolicitudService tokenRespuestaSolicitudService;
    @Mock
    private AlmacenamientoArchivosService almacenamientoArchivosService;

    private static final String URL_BASE_RESPONDER_SOLICITUD = "http://localhost:4200/solicitudes/responder";

    private SolicitudSocioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SolicitudSocioServiceImpl(solicitudSocioRepository, usuarioRepository, secuenciaService, emailService,
                socioService, tokenRespuestaSolicitudService, almacenamientoArchivosService, URL_BASE_RESPONDER_SOLICITUD);
    }

    private SolicitudSocioRequest requestFisicaValida() {
        return new SolicitudSocioRequest(
                CategoriaSocio.ACTIVO, TipoPersona.FISICA, "García, Juan Carlos", "28345678",
                "20-28345678-2", LocalDate.of(1985, 4, 12), "Calle 123", "Depto B",
                "+54 9 3777123456", "juan.garcia@example.com", "Farmacia Central", "Ruta 123 km 4",
                null, null, true);
    }

    private SolicitudSocioRequest requestJuridicaValida() {
        return new SolicitudSocioRequest(
                CategoriaSocio.ADHERENTE, TipoPersona.JURIDICA, "Agropecuaria Curuzú S.A.", null,
                "30-71234567-8", null, "Ruta 123", null,
                "+54 9 3777123456", "contacto@agropecuaria.com", "Agropecuaria Curuzú", "Ruta 123 km 4",
                "María Fernández", "30123456", true);
    }

    @Test
    void crear_conPersonaFisica_guardaLaSolicitudPendienteYMandaConfirmacion() {
        SolicitudSocioRequest request = requestFisicaValida();

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(solicitudSocioRepository.existsByEmailIgnoreCaseAndEstadoIn(anyString(), any())).thenReturn(false);
        when(solicitudSocioRepository.existsByDocumentosInAndEstadoIn(any(), any())).thenReturn(false);
        when(secuenciaService.siguienteValor(SolicitudSocioConstantes.NOMBRE_SECUENCIA_NUMERO_SOLICITUD)).thenReturn(123L);

        SolicitudSocioCreadaResponse response = service.crearSolicitudSocio(request);

        assertThat(response.mensaje()).isEqualTo("Solicitud de socio enviada con éxito");
        assertThat(response.solicitud().numeroSolicitud()).isEqualTo("SOL-000123");
        assertThat(response.solicitud().estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(response.solicitud().historial()).hasSize(1);

        verify(solicitudSocioRepository).save(any(SolicitudSocio.class));
        verify(emailService).enviarCorreoConfirmacionSolicitudSocio(
                eq("juan.garcia@example.com"), anyString(), eq("SOL-000123"));
    }

    @Test
    void crear_conPersonaJuridica_guardaLaSolicitudPendiente() {
        SolicitudSocioRequest request = requestJuridicaValida();

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(solicitudSocioRepository.existsByEmailIgnoreCaseAndEstadoIn(anyString(), any())).thenReturn(false);
        when(solicitudSocioRepository.existsByDocumentosInAndEstadoIn(any(), any())).thenReturn(false);
        when(secuenciaService.siguienteValor(SolicitudSocioConstantes.NOMBRE_SECUENCIA_NUMERO_SOLICITUD)).thenReturn(7L);

        SolicitudSocioCreadaResponse response = service.crearSolicitudSocio(request);

        assertThat(response.solicitud().numeroSolicitud()).isEqualTo("SOL-000007");
        assertThat(response.solicitud().tipoPersona()).isEqualTo(TipoPersona.JURIDICA);
        assertThat(response.solicitud().datosPersonaJuridica().getRazonSocial()).isEqualTo("Agropecuaria Curuzú S.A.");
    }

    @Test
    void crear_conEmailYaRegistradoComoUsuario_lanzaExcepcion() {
        SolicitudSocioRequest request = requestFisicaValida();

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.crearSolicitudSocio(request)).isInstanceOf(EmailYaRegistradoException.class);
        verify(solicitudSocioRepository, never()).save(any());
    }

    @Test
    void crear_conDocumentoYaRegistradoEnOtraSolicitud_lanzaExcepcion() {
        SolicitudSocioRequest request = requestFisicaValida();

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
        verify(socioService).crearSocioDesdeSolicitud(solicitud, "admin-1", "Admin Uno");
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
    void cambiarEstado_deAprobadaAEnRevision_esUnaTransicionInvalida() {
        SolicitudSocio solicitud = solicitudPendiente();
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));

        CambiarEstadoSolicitudRequest request = new CambiarEstadoSolicitudRequest(
                EstadoSolicitud.EN_REVISION, null, null);

        assertThatThrownBy(() -> service.cambiarEstadoSolicitudSocio("SOL-000001", request, "admin-1", "Admin Uno"))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void cambiarEstado_deCanceladaAEnRevision_esUnaTransicionInvalida() {
        SolicitudSocio solicitud = solicitudPendiente();
        solicitud.setEstado(EstadoSolicitud.CANCELADA);
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));

        CambiarEstadoSolicitudRequest request = new CambiarEstadoSolicitudRequest(
                EstadoSolicitud.EN_REVISION, null, null);

        assertThatThrownBy(() -> service.cambiarEstadoSolicitudSocio("SOL-000001", request, "admin-1", "Admin Uno"))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void cambiarEstado_dePendienteAEnRevision_esUnaTransicionValida() {
        SolicitudSocio solicitud = solicitudPendiente();
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));

        CambiarEstadoSolicitudRequest request = new CambiarEstadoSolicitudRequest(
                EstadoSolicitud.EN_REVISION, "Se pone en revisión", null);

        CambiarEstadoSolicitudResponse response = service.cambiarEstadoSolicitudSocio("SOL-000001", request, "admin-1", "Admin Uno");

        assertThat(response.estado()).isEqualTo(EstadoSolicitud.EN_REVISION);
        verify(solicitudSocioRepository).save(solicitud);
    }

    @Test
    void cambiarEstado_dePendienteACancelada_esUnaTransicionInvalida() {
        SolicitudSocio solicitud = solicitudPendiente();
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));

        CambiarEstadoSolicitudRequest request = new CambiarEstadoSolicitudRequest(
                EstadoSolicitud.CANCELADA, null, "motivo");

        assertThatThrownBy(() -> service.cambiarEstadoSolicitudSocio("SOL-000001", request, "admin-1", "Admin Uno"))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
        verify(solicitudSocioRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_dePendienteARechazada_esUnaTransicionInvalida() {
        SolicitudSocio solicitud = solicitudPendiente();
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));

        CambiarEstadoSolicitudRequest request = new CambiarEstadoSolicitudRequest(
                EstadoSolicitud.RECHAZADA, null, "motivo");

        assertThatThrownBy(() -> service.cambiarEstadoSolicitudSocio("SOL-000001", request, "admin-1", "Admin Uno"))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
        verify(solicitudSocioRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_aRechazadaSinMotivo_esInvalido() {
        SolicitudSocio solicitud = solicitudPendiente();
        solicitud.setEstado(EstadoSolicitud.EN_REVISION);
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
        solicitud.setEstado(EstadoSolicitud.EN_REVISION);
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
        verify(socioService, never()).crearSocioDesdeSolicitud(any(), anyString(), anyString());
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

    @Test
    void agregarObservacion_noCambiaElEstadoYQuedaEnElHistorial() {
        SolicitudSocio solicitud = solicitudPendiente();
        solicitud.setEstado(EstadoSolicitud.EN_REVISION);
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));
        when(tokenRespuestaSolicitudService.generar("SOL-000001")).thenReturn("token-plano-abc");

        ObservacionAgregadaResponse response = service.agregarObservacion(
                "SOL-000001", "Falta el comprobante de domicilio", "admin-1", "Admin Uno");

        assertThat(response.numeroSolicitud()).isEqualTo("SOL-000001");
        assertThat(response.mensaje()).isEqualTo("Observación agregada correctamente");
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.EN_REVISION);
        assertThat(solicitud.getHistorial())
                .anyMatch(h -> "Falta el comprobante de domicilio".equals(h.getObservacion())
                        && h.getEstadoAnterior() == EstadoSolicitud.EN_REVISION
                        && h.getEstadoNuevo() == EstadoSolicitud.EN_REVISION);
        verify(solicitudSocioRepository).save(solicitud);
        verify(emailService).enviarCorreoObservacionSolicitudSocio(
                "juan.garcia@example.com", "García, Juan Carlos", "SOL-000001", "Falta el comprobante de domicilio",
                "http://localhost:4200/solicitudes/responder?token=token-plano-abc");
    }

    @Test
    void agregarObservacion_solicitudInexistente_lanzaExcepcion() {
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agregarObservacion("SOL-999999", "obs", "admin-1", "Admin Uno"))
                .isInstanceOf(SolicitudNoEncontradaException.class);
    }

    @Test
    void consultarObservacionPendiente_conTokenValido_devuelveLaUltimaObservacionDeAdmin() {
        when(tokenRespuestaSolicitudService.validarYObtenerNumeroSolicitud("token-abc")).thenReturn("SOL-000001");
        SolicitudSocio solicitud = solicitudPendiente();

        CambioEstadoSolicitud observacionVieja = new CambioEstadoSolicitud();
        observacionVieja.setAdminResponsableId("admin-1");
        observacionVieja.setObservacion("Falta el DNI");
        observacionVieja.setFechaHora(Instant.parse("2026-01-01T10:00:00Z"));
        solicitud.getHistorial().add(observacionVieja);

        CambioEstadoSolicitud observacionReciente = new CambioEstadoSolicitud();
        observacionReciente.setAdminResponsableId("admin-1");
        observacionReciente.setObservacion("Falta el comprobante de domicilio");
        observacionReciente.setFechaHora(Instant.parse("2026-02-01T10:00:00Z"));
        solicitud.getHistorial().add(observacionReciente);

        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));

        ObservacionPendienteResponse response = service.consultarObservacionPendiente("token-abc");

        assertThat(response.numeroSolicitud()).isEqualTo("SOL-000001");
        assertThat(response.nombreSolicitante()).isEqualTo("García, Juan Carlos");
        assertThat(response.observacion()).isEqualTo("Falta el comprobante de domicilio");
    }

    @Test
    void consultarObservacionPendiente_tokenInvalido_propagaLaExcepcion() {
        when(tokenRespuestaSolicitudService.validarYObtenerNumeroSolicitud("token-invalido"))
                .thenThrow(new TokenRespuestaSolicitudInvalidoException());

        assertThatThrownBy(() -> service.consultarObservacionPendiente("token-invalido"))
                .isInstanceOf(TokenRespuestaSolicitudInvalidoException.class);
        verify(solicitudSocioRepository, never()).findByNumeroSolicitud(anyString());
    }

    @Test
    void responderObservacion_conArchivos_losGuardaMarcaElTokenUsadoYAvisaATodosLosAdmins() {
        when(tokenRespuestaSolicitudService.validarYObtenerNumeroSolicitud("token-abc")).thenReturn("SOL-000001");
        SolicitudSocio solicitud = solicitudPendiente();
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));

        MultipartFile archivo = new MockMultipartFile(
                "archivo", "comprobante.pdf", "application/pdf", "contenido".getBytes());
        List<MultipartFile> archivos = List.of(archivo);
        List<String> rutasGuardadas = List.of("SOL-000001/uuid-1_comprobante.pdf");
        when(almacenamientoArchivosService.guardarTodos("SOL-000001", archivos)).thenReturn(rutasGuardadas);

        Usuario admin1 = Usuario.builder().email("admin1@ruralcuruzu.com").rol(Rol.ADMIN).build();
        Usuario admin2 = Usuario.builder().email("admin2@ruralcuruzu.com").rol(Rol.ADMIN).build();
        when(usuarioRepository.findByRol(Rol.ADMIN)).thenReturn(List.of(admin1, admin2));

        service.responderObservacion("token-abc", "Acá va el comprobante que faltaba", archivos);

        assertThat(solicitud.getHistorial()).anyMatch(h ->
                "Acá va el comprobante que faltaba".equals(h.getObservacion())
                        && h.getAdminResponsableId() == null
                        && rutasGuardadas.equals(h.getArchivosAdjuntos()));
        verify(solicitudSocioRepository).save(solicitud);
        verify(tokenRespuestaSolicitudService).marcarComoUsado("token-abc");
        verify(emailService).enviarCorreoRespuestaSolicitudRecibida(
                "admin1@ruralcuruzu.com", "SOL-000001", "García, Juan Carlos", true);
        verify(emailService).enviarCorreoRespuestaSolicitudRecibida(
                "admin2@ruralcuruzu.com", "SOL-000001", "García, Juan Carlos", true);
    }

    @Test
    void responderObservacion_sinArchivos_noLlamaAlAlmacenamientoYAvisaSinArchivos() {
        when(tokenRespuestaSolicitudService.validarYObtenerNumeroSolicitud("token-abc")).thenReturn("SOL-000001");
        SolicitudSocio solicitud = solicitudPendiente();
        when(solicitudSocioRepository.findByNumeroSolicitud("SOL-000001")).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findByRol(Rol.ADMIN)).thenReturn(List.of());

        service.responderObservacion("token-abc", "No tengo archivos para adjuntar", List.of());

        verify(almacenamientoArchivosService, never()).guardarTodos(anyString(), any());
        assertThat(solicitud.getHistorial()).anyMatch(h ->
                "No tengo archivos para adjuntar".equals(h.getObservacion())
                        && h.getArchivosAdjuntos().isEmpty());
        verify(tokenRespuestaSolicitudService).marcarComoUsado("token-abc");
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
                "García, Juan Carlos", "28345678", LocalDate.of(1985, 4, 12), "20-28345678-2",
                "Calle 123", "Depto B", "+54 9 3777123456", "juan.garcia@example.com", null, null));
        return solicitud;
    }
}
