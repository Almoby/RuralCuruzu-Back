package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.almoby.ruralcuruzu.constantes.SolicitudSocioConstantes;
import com.almoby.ruralcuruzu.domain.CambioEstadoSolicitud;
import com.almoby.ruralcuruzu.domain.DatosPersonaFisica;
import com.almoby.ruralcuruzu.domain.DatosPersonaJuridica;
import com.almoby.ruralcuruzu.domain.SolicitudSocio;
import com.almoby.ruralcuruzu.domain.Usuario;
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
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioResumenResponse;
import com.almoby.ruralcuruzu.exception.DocumentoYaRegistradoException;
import com.almoby.ruralcuruzu.exception.EmailYaRegistradoException;
import com.almoby.ruralcuruzu.exception.SolicitudNoEncontradaException;
import com.almoby.ruralcuruzu.exception.TransicionEstadoInvalidaException;
import com.almoby.ruralcuruzu.repository.SolicitudSocioRepository;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.service.AlmacenamientoArchivosService;
import com.almoby.ruralcuruzu.service.EmailService;
import com.almoby.ruralcuruzu.service.SecuenciaService;
import com.almoby.ruralcuruzu.service.SocioService;
import com.almoby.ruralcuruzu.service.SolicitudSocioService;
import com.almoby.ruralcuruzu.service.TokenRespuestaSolicitudService;

import lombok.extern.slf4j.Slf4j;

/**
 * Ver documento, secciones 4 y 5 ("Solicitud para ser socio"). Reglas clave:
 * - Al crear, NO se crea ningún Usuario habilitado (5.4): la solicitud queda
 *   PENDIENTE hasta que un admin la revise.
 * - Los duplicados de email/documento solo se chequean contra solicitudes
 *   "vivas" (no RECHAZADA/CANCELADA) y contra cuentas de Usuario ya existentes:
 *   una solicitud rechazada o cancelada no debe bloquear un nuevo intento.
 * - Las transiciones de estado están restringidas a un pequeño grafo (ver
 *   TRANSICIONES_VALIDAS). Desde PENDIENTE solo se puede pasar a EN_REVISION
 *   (no se aprueba/rechaza/cancela directo sin pasar por revisión). APROBADA
 *   y CANCELADA son estados finales de verdad (no hay vuelta atrás). RECHAZADA
 *   es la única excepción: se puede "reabrir" y volver a EN_REVISION.
 * - Al rechazar, además del motivo obligatorio, se manda un correo al
 *   solicitante avisando el motivo.
 * - Al aprobar (documento, sección 8.4), se delega en SocioService la creación
 *   del Socio y su Usuario con contraseña temporal: esta clase solo dispara
 *   ese alta, no conoce los detalles de cómo se arma un Socio.
 */
@Slf4j
@Service
public class SolicitudSocioServiceImpl implements SolicitudSocioService {

    private static final Set<EstadoSolicitud> ESTADOS_QUE_BLOQUEAN_DUPLICADOS =
            EnumSet.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.EN_REVISION, EstadoSolicitud.APROBADA);

    /**
     * PENDIENTE solo puede pasar a EN_REVISION: una solicitud recién creada
     * tiene que pasar primero por esa revisión antes de poder aprobarse,
     * rechazarse o cancelarse directamente. APROBADA y CANCELADA son estados
     * finales de verdad: no hay transición posible desde ahí. RECHAZADA es la
     * única excepción (documento, sección de Rechazo: "podrá volver a abrirse
     * posteriormente") y solo puede reabrirse hacia EN_REVISION (no a
     * PENDIENTE: ya pasó por una primera revisión).
     */
    private static final Map<EstadoSolicitud, Set<EstadoSolicitud>> TRANSICIONES_VALIDAS = new EnumMap<>(Map.of(
            EstadoSolicitud.PENDIENTE, EnumSet.of(EstadoSolicitud.EN_REVISION),
            EstadoSolicitud.EN_REVISION, EnumSet.of(EstadoSolicitud.APROBADA, EstadoSolicitud.RECHAZADA, EstadoSolicitud.CANCELADA),
            EstadoSolicitud.APROBADA, EnumSet.noneOf(EstadoSolicitud.class),
            EstadoSolicitud.RECHAZADA, EnumSet.of(EstadoSolicitud.EN_REVISION),
            EstadoSolicitud.CANCELADA, EnumSet.noneOf(EstadoSolicitud.class)));

    private static final Set<EstadoSolicitud> ESTADOS_QUE_REQUIEREN_MOTIVO =
            EnumSet.of(EstadoSolicitud.RECHAZADA, EstadoSolicitud.CANCELADA);

    private final SolicitudSocioRepository solicitudSocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecuenciaService secuenciaService;
    private final EmailService emailService;
    private final SocioService socioService;
    private final TokenRespuestaSolicitudService tokenRespuestaSolicitudService;
    private final AlmacenamientoArchivosService almacenamientoArchivosService;
    private final String urlBaseResponderSolicitud;

    public SolicitudSocioServiceImpl(SolicitudSocioRepository solicitudSocioRepository,
                                      UsuarioRepository usuarioRepository,
                                      SecuenciaService secuenciaService,
                                      EmailService emailService,
                                      SocioService socioService,
                                      TokenRespuestaSolicitudService tokenRespuestaSolicitudService,
                                      AlmacenamientoArchivosService almacenamientoArchivosService,
                                      @Value("${app.frontend.responder-solicitud-url:http://localhost:4200/solicitudes/responder}")
                                      String urlBaseResponderSolicitud) {
        this.solicitudSocioRepository = solicitudSocioRepository;
        this.usuarioRepository = usuarioRepository;
        this.secuenciaService = secuenciaService;
        this.emailService = emailService;
        this.socioService = socioService;
        this.tokenRespuestaSolicitudService = tokenRespuestaSolicitudService;
        this.almacenamientoArchivosService = almacenamientoArchivosService;
        this.urlBaseResponderSolicitud = urlBaseResponderSolicitud;
    }

    @Override
    public SolicitudSocioCreadaResponse crearSolicitudSocio(SolicitudSocioRequest request) {
        String email = request.email().trim().toLowerCase();
        List<String> documentos = extraerDocumentos(request);

        validarNoDuplicado(email, documentos);

        String numeroSolicitud = SolicitudSocioConstantes.PREFIJO_NUMERO_SOLICITUD
                + String.format("%06d", secuenciaService.siguienteValor(SolicitudSocioConstantes.NOMBRE_SECUENCIA_NUMERO_SOLICITUD));
        Instant ahora = Instant.now();

        SolicitudSocio solicitud = SolicitudSocio.builder()
                .numeroSolicitud(numeroSolicitud)
                .categoriaSolicitada(request.categoriaSolicitada())
                .tipoPersona(request.tipoPersona())
                .datosPersonaFisica(request.tipoPersona() == TipoPersona.FISICA ? mapearFisica(request) : null)
                .datosPersonaJuridica(request.tipoPersona() == TipoPersona.JURIDICA ? mapearJuridica(request) : null)
                .email(email)
                .documentos(documentos)
                .aceptaTerminosYCondiciones(request.aceptaTerminosYCondiciones())
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .build();

        CambioEstadoSolicitud alta = new CambioEstadoSolicitud();
        alta.setEstadoAnterior(null);
        alta.setEstadoNuevo(EstadoSolicitud.PENDIENTE);
        alta.setFechaHora(ahora);
        alta.setObservacion("Solicitud creada por el solicitante");
        solicitud.getHistorial().add(alta);

        solicitudSocioRepository.save(solicitud);
        log.info("Solicitud de socio creada numeroSolicitud={} email={} categoria={} tipoPersona={}",
                numeroSolicitud, email, request.categoriaSolicitada(), request.tipoPersona());

        String nombreParaSaludo = solicitud.nombreParaMostrar();
        emailService.enviarCorreoConfirmacionSolicitudSocio(email, nombreParaSaludo, numeroSolicitud);

        return SolicitudSocioCreadaResponse.of(SolicitudSocioResponse.from(solicitud));
    }

    @Override
    public List<SolicitudSocioResumenResponse> listarSolicitudesSocio(EstadoSolicitud estado) {
        List<SolicitudSocio> solicitudes = estado != null
                ? solicitudSocioRepository.findByEstado(estado)
                : solicitudSocioRepository.findAll();

        return solicitudes.stream().map(SolicitudSocioResumenResponse::from).toList();
    }

    @Override
    public SolicitudSocioResponse obtenerSolicitudSocioPorNumero(String numeroSolicitud) {
        return SolicitudSocioResponse.from(buscarOFallar(numeroSolicitud));
    }

    @Override
    public CambiarEstadoSolicitudResponse cambiarEstadoSolicitudSocio(String numeroSolicitud, CambiarEstadoSolicitudRequest request,
                                                                       String adminId, String adminNombre) {
        SolicitudSocio solicitud = buscarOFallar(numeroSolicitud);
        EstadoSolicitud estadoActual = solicitud.getEstado();
        EstadoSolicitud nuevoEstado = request.nuevoEstado();

        if (!TRANSICIONES_VALIDAS.getOrDefault(estadoActual, Set.of()).contains(nuevoEstado)) {
            log.warn("Transición inválida numeroSolicitud={} de {} a {}", numeroSolicitud, estadoActual, nuevoEstado);
            throw new TransicionEstadoInvalidaException(estadoActual, nuevoEstado);
        }

        if (ESTADOS_QUE_REQUIEREN_MOTIVO.contains(nuevoEstado) && (request.motivo() == null || request.motivo().isBlank())) {
            throw new TransicionEstadoInvalidaException(
                    "El motivo es obligatorio para pasar una solicitud a " + nuevoEstado);
        }

        Instant ahora = Instant.now();
        CambioEstadoSolicitud cambio = new CambioEstadoSolicitud();
        cambio.setEstadoAnterior(estadoActual);
        cambio.setEstadoNuevo(nuevoEstado);
        cambio.setFechaHora(ahora);
        cambio.setAdminResponsableId(adminId);
        cambio.setAdminResponsableNombre(adminNombre);
        cambio.setObservacion(request.observacion());
        cambio.setMotivo(request.motivo());

        solicitud.setEstado(nuevoEstado);
        solicitud.setFechaActualizacion(ahora);
        solicitud.getHistorial().add(cambio);

        solicitudSocioRepository.save(solicitud);
        log.info("Solicitud de socio numeroSolicitud={} pasó de {} a {} (admin={})",
                numeroSolicitud, estadoActual, nuevoEstado, adminNombre);

        if (nuevoEstado == EstadoSolicitud.RECHAZADA) {
            emailService.enviarCorreoRechazoSolicitudSocio(
                    solicitud.getEmail(), solicitud.nombreParaMostrar(), numeroSolicitud, request.motivo());
        }

        if (nuevoEstado == EstadoSolicitud.APROBADA) {
            socioService.crearSocioDesdeSolicitud(solicitud, adminId, adminNombre);
        }

        return CambiarEstadoSolicitudResponse.of(numeroSolicitud, nuevoEstado);
    }

    @Override
    public ObservacionAgregadaResponse agregarObservacion(String numeroSolicitud, String observacion,
                                                            String adminId, String adminNombre) {
        SolicitudSocio solicitud = buscarOFallar(numeroSolicitud);
        Instant ahora = Instant.now();

        CambioEstadoSolicitud entrada = new CambioEstadoSolicitud();
        entrada.setEstadoAnterior(solicitud.getEstado());
        entrada.setEstadoNuevo(solicitud.getEstado());
        entrada.setFechaHora(ahora);
        entrada.setAdminResponsableId(adminId);
        entrada.setAdminResponsableNombre(adminNombre);
        entrada.setObservacion(observacion);

        solicitud.getHistorial().add(entrada);
        solicitud.setFechaActualizacion(ahora);
        solicitudSocioRepository.save(solicitud);

        log.info("Observación agregada a numeroSolicitud={} (admin={})", numeroSolicitud, adminNombre);

        String tokenPlano = tokenRespuestaSolicitudService.generar(numeroSolicitud);
        String enlaceRespuesta = urlBaseResponderSolicitud + "?token=" + tokenPlano;
        emailService.enviarCorreoObservacionSolicitudSocio(
                solicitud.getEmail(), solicitud.nombreParaMostrar(), numeroSolicitud, observacion, enlaceRespuesta);

        return ObservacionAgregadaResponse.of(numeroSolicitud);
    }

    @Override
    public ObservacionPendienteResponse consultarObservacionPendiente(String tokenPlano) {
        String numeroSolicitud = tokenRespuestaSolicitudService.validarYObtenerNumeroSolicitud(tokenPlano);
        SolicitudSocio solicitud = buscarOFallar(numeroSolicitud);

        CambioEstadoSolicitud ultimaObservacionDeAdmin = solicitud.getHistorial().stream()
                .filter(cambio -> cambio.getAdminResponsableId() != null && cambio.getObservacion() != null)
                .max(Comparator.comparing(CambioEstadoSolicitud::getFechaHora))
                .orElse(null);

        return new ObservacionPendienteResponse(
                numeroSolicitud,
                solicitud.nombreParaMostrar(),
                ultimaObservacionDeAdmin != null ? ultimaObservacionDeAdmin.getObservacion() : null,
                ultimaObservacionDeAdmin != null ? ultimaObservacionDeAdmin.getFechaHora() : null);
    }

    @Override
    public void responderObservacion(String tokenPlano, String texto, List<MultipartFile> archivos) {
        String numeroSolicitud = tokenRespuestaSolicitudService.validarYObtenerNumeroSolicitud(tokenPlano);
        SolicitudSocio solicitud = buscarOFallar(numeroSolicitud);

        List<String> archivosGuardados = (archivos == null || archivos.isEmpty())
                ? List.of()
                : almacenamientoArchivosService.guardarTodos(numeroSolicitud, archivos);

        Instant ahora = Instant.now();
        CambioEstadoSolicitud entrada = new CambioEstadoSolicitud();
        entrada.setEstadoAnterior(solicitud.getEstado());
        entrada.setEstadoNuevo(solicitud.getEstado());
        entrada.setFechaHora(ahora);
        entrada.setObservacion(texto);
        entrada.setArchivosAdjuntos(archivosGuardados);
        // adminResponsableId queda null a propósito: este cambio lo generó el
        // propio solicitante, no un admin (mismo criterio que el alta inicial).

        solicitud.getHistorial().add(entrada);
        solicitud.setFechaActualizacion(ahora);
        solicitudSocioRepository.save(solicitud);

        // El token es de un solo uso: se consume recién acá, después de guardar
        // con éxito, para no invalidarlo si algo falla antes.
        tokenRespuestaSolicitudService.marcarComoUsado(tokenPlano);

        log.info("Respuesta de solicitante recibida numeroSolicitud={} archivos={}",
                numeroSolicitud, archivosGuardados.size());

        boolean tieneArchivos = !archivosGuardados.isEmpty();
        usuarioRepository.findByRol(Rol.ADMIN).forEach(admin ->
                emailService.enviarCorreoRespuestaSolicitudRecibida(
                        admin.getEmail(), numeroSolicitud, solicitud.nombreParaMostrar(), tieneArchivos));
    }

    private SolicitudSocio buscarOFallar(String numeroSolicitud) {
        return solicitudSocioRepository.findByNumeroSolicitud(numeroSolicitud)
                .orElseThrow(() -> new SolicitudNoEncontradaException(numeroSolicitud));
    }

    private void validarNoDuplicado(String email, List<String> documentos) {
        boolean emailRegistrado = usuarioRepository.existsByEmail(email)
                || solicitudSocioRepository.existsByEmailIgnoreCaseAndEstadoIn(email, ESTADOS_QUE_BLOQUEAN_DUPLICADOS);
        if (emailRegistrado) {
            log.warn("Solicitud de socio rechazada: email ya registrado ({})", email);
            throw new EmailYaRegistradoException();
        }

        if (solicitudSocioRepository.existsByDocumentosInAndEstadoIn(documentos, ESTADOS_QUE_BLOQUEAN_DUPLICADOS)) {
            log.warn("Solicitud de socio rechazada: documento ya registrado (alguno de {})", documentos);
            throw new DocumentoYaRegistradoException();
        }
    }

    /**
     * Persona física aporta DOS documentos identificatorios (DNI y CUIL); persona
     * jurídica, uno solo (CUIT). Hay que chequear duplicados contra los dos del
     * lado física: si solo se validara el DNI, alguien podría mandar un DNI
     * distinto pero reusar el CUIL de una solicitud ya existente.
     */
    private List<String> extraerDocumentos(SolicitudSocioRequest request) {
        if (request.tipoPersona() == TipoPersona.FISICA) {
            String dni = normalizarDocumento(request.documento());
            String cuil = normalizarDocumento(request.cuit());
            return List.of(dni, cuil);
        }
        return List.of(normalizarDocumento(request.cuit()));
    }

    private String normalizarDocumento(String valor) {
        return valor.replaceAll("[-.\\s]", "");
    }

    private DatosPersonaFisica mapearFisica(SolicitudSocioRequest r) {
        return new DatosPersonaFisica(r.apellidoYNombre(), r.documento(), r.fechaNacimiento(), r.cuit(),
                r.direccion(), r.portalPisoDepartamento(), r.telefono(), r.email().trim().toLowerCase(),
                r.nombreEstablecimiento(), r.direccionEstablecimiento());
    }

    private DatosPersonaJuridica mapearJuridica(SolicitudSocioRequest r) {
        return new DatosPersonaJuridica(r.apellidoYNombre(), r.cuit(), r.direccion(), r.portalPisoDepartamento(),
                r.telefono(), r.email().trim().toLowerCase(), r.nombreEstablecimiento(), r.nombreResponsable(),
                r.dniResponsable(), r.direccionEstablecimiento());
    }
}
