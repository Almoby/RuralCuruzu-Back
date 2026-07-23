package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.constantes.SolicitudSocioConstantes;
import com.almoby.ruralcuruzu.domain.CambioEstadoSolicitud;
import com.almoby.ruralcuruzu.domain.DatosPersonaFisica;
import com.almoby.ruralcuruzu.domain.DatosPersonaJuridica;
import com.almoby.ruralcuruzu.domain.SolicitudSocio;
import com.almoby.ruralcuruzu.enums.EstadoSolicitud;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoSolicitudRequest;
import com.almoby.ruralcuruzu.dto.request.DatosPersonaFisicaRequest;
import com.almoby.ruralcuruzu.dto.request.DatosPersonaJuridicaRequest;
import com.almoby.ruralcuruzu.dto.request.SolicitudSocioRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoSolicitudResponse;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioResponse;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioResumenResponse;
import com.almoby.ruralcuruzu.exception.DocumentoYaRegistradoException;
import com.almoby.ruralcuruzu.exception.EmailYaRegistradoException;
import com.almoby.ruralcuruzu.exception.SolicitudNoEncontradaException;
import com.almoby.ruralcuruzu.exception.TransicionEstadoInvalidaException;
import com.almoby.ruralcuruzu.repository.SolicitudSocioRepository;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.service.EmailService;
import com.almoby.ruralcuruzu.service.SecuenciaService;
import com.almoby.ruralcuruzu.service.SolicitudSocioService;

import lombok.extern.slf4j.Slf4j;

/**
 * Ver documento, secciones 4 y 5 ("Solicitud para ser socio"). Reglas clave:
 * - Al crear, NO se crea ningún Usuario habilitado (5.4): la solicitud queda
 *   PENDIENTE hasta que un admin la revise.
 * - Los duplicados de email/documento solo se chequean contra solicitudes
 *   "vivas" (no RECHAZADA/CANCELADA) y contra cuentas de Usuario ya existentes:
 *   una solicitud rechazada o cancelada no debe bloquear un nuevo intento.
 * - Las transiciones de estado están restringidas a un pequeño grafo (ver
 *   TRANSICIONES_VALIDAS): no se puede, por ejemplo, aprobar directamente una
 *   solicitud RECHAZADA (primero hay que reabrirla a EN_REVISION).
 * - Rechazar/aprobar/cancelar no son terminales de verdad: desde cualquiera de
 *   esos tres estados se puede "reabrir" la solicitud, que vuelve a EN_REVISION.
 * - Al rechazar, además del motivo obligatorio, se manda un correo al
 *   solicitante avisando el motivo.
 * - Qué pasa con la cuenta de Usuario cuando una solicitud se APRUEBA todavía
 *   NO está resuelto acá a propósito: ese alta se va a definir junto con el
 *   módulo de Gestión de Socios (por ahora no existe un documento Socio al
 *   cual asociarla).
 */
@Slf4j
@Service
public class SolicitudSocioServiceImpl implements SolicitudSocioService {

    private static final Set<EstadoSolicitud> ESTADOS_QUE_BLOQUEAN_DUPLICADOS =
            EnumSet.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.EN_REVISION, EstadoSolicitud.APROBADA);

    /**
     * APROBADA, RECHAZADA y CANCELADA no son "terminales" del todo: el documento
     * (sección de Rechazo) exige poder reabrir una solicitud rechazada más
     * adelante, y por consistencia se permite lo mismo desde aprobada/cancelada.
     * Reabrir siempre vuelve a EN_REVISION (no a PENDIENTE): la solicitud ya
     * pasó por una primera revisión, no es una solicitud nueva.
     */
    private static final Map<EstadoSolicitud, Set<EstadoSolicitud>> TRANSICIONES_VALIDAS = new EnumMap<>(Map.of(
            EstadoSolicitud.PENDIENTE, EnumSet.of(EstadoSolicitud.EN_REVISION, EstadoSolicitud.RECHAZADA, EstadoSolicitud.CANCELADA),
            EstadoSolicitud.EN_REVISION, EnumSet.of(EstadoSolicitud.APROBADA, EstadoSolicitud.RECHAZADA, EstadoSolicitud.CANCELADA),
            EstadoSolicitud.APROBADA, EnumSet.of(EstadoSolicitud.EN_REVISION),
            EstadoSolicitud.RECHAZADA, EnumSet.of(EstadoSolicitud.EN_REVISION),
            EstadoSolicitud.CANCELADA, EnumSet.of(EstadoSolicitud.EN_REVISION)));

    private static final Set<EstadoSolicitud> ESTADOS_QUE_REQUIEREN_MOTIVO =
            EnumSet.of(EstadoSolicitud.RECHAZADA, EstadoSolicitud.CANCELADA);

    private final SolicitudSocioRepository solicitudSocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecuenciaService secuenciaService;
    private final EmailService emailService;

    public SolicitudSocioServiceImpl(SolicitudSocioRepository solicitudSocioRepository,
                                      UsuarioRepository usuarioRepository,
                                      SecuenciaService secuenciaService,
                                      EmailService emailService) {
        this.solicitudSocioRepository = solicitudSocioRepository;
        this.usuarioRepository = usuarioRepository;
        this.secuenciaService = secuenciaService;
        this.emailService = emailService;
    }

    @Override
    public SolicitudSocioResponse crearSolicitudSocio(SolicitudSocioRequest request) {
        String email = extraerEmail(request).trim().toLowerCase();
        List<String> documentos = extraerDocumentos(request);

        validarNoDuplicado(email, documentos);

        String numeroSolicitud = SolicitudSocioConstantes.PREFIJO_NUMERO_SOLICITUD
                + String.format("%06d", secuenciaService.siguienteValor(SolicitudSocioConstantes.NOMBRE_SECUENCIA_NUMERO_SOLICITUD));
        Instant ahora = Instant.now();

        SolicitudSocio solicitud = SolicitudSocio.builder()
                .numeroSolicitud(numeroSolicitud)
                .categoriaSolicitada(request.categoriaSolicitada())
                .tipoPersona(request.tipoPersona())
                .datosPersonaFisica(request.tipoPersona() == TipoPersona.FISICA ? mapear(request.datosPersonaFisica()) : null)
                .datosPersonaJuridica(request.tipoPersona() == TipoPersona.JURIDICA ? mapear(request.datosPersonaJuridica()) : null)
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

        return SolicitudSocioResponse.from(solicitud);
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

        return CambiarEstadoSolicitudResponse.of(numeroSolicitud, nuevoEstado);
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

    private String extraerEmail(SolicitudSocioRequest request) {
        return request.tipoPersona() == TipoPersona.FISICA
                ? request.datosPersonaFisica().email()
                : request.datosPersonaJuridica().email();
    }

    /**
     * Persona física aporta DOS documentos identificatorios (DNI y CUIL); persona
     * jurídica, uno solo (CUIT). Hay que chequear duplicados contra los dos del
     * lado física: si solo se validara el DNI, alguien podría mandar un DNI
     * distinto pero reusar el CUIL de una solicitud ya existente.
     */
    private List<String> extraerDocumentos(SolicitudSocioRequest request) {
        if (request.tipoPersona() == TipoPersona.FISICA) {
            String dni = normalizarDocumento(request.datosPersonaFisica().dni());
            String cuil = normalizarDocumento(request.datosPersonaFisica().cuitCuil());
            return List.of(dni, cuil);
        }
        return List.of(normalizarDocumento(request.datosPersonaJuridica().cuit()));
    }

    private String normalizarDocumento(String valor) {
        return valor.replaceAll("[-.\\s]", "");
    }

    private DatosPersonaFisica mapear(DatosPersonaFisicaRequest r) {
        return new DatosPersonaFisica(r.nombre(), r.apellido(), r.dni(), r.fechaNacimiento(), r.cuitCuil(),
                r.direccion(), r.portalPisoDepartamento(), r.telefono(), r.email(), r.ocupacion(), r.nombreEstablecimiento());
    }

    private DatosPersonaJuridica mapear(DatosPersonaJuridicaRequest r) {
        return new DatosPersonaJuridica(r.razonSocial(), r.cuit(), r.direccion(), r.portalPisoDepartamento(),
                r.telefono(), r.email(), r.nombreEstablecimiento(), r.nombreResponsable(), r.dniResponsable());
    }
}
