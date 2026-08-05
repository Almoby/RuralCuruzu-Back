package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.constantes.SocioConstantes;
import com.almoby.ruralcuruzu.domain.DatosPersonaFisica;
import com.almoby.ruralcuruzu.domain.DatosPersonaJuridica;
import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.domain.SolicitudSocio;
import com.almoby.ruralcuruzu.dto.request.ActualizarSocioParcialRequest;
import com.almoby.ruralcuruzu.dto.request.AltaManualSocioRequest;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoSocioRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoSocioResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoQrResponse;
import com.almoby.ruralcuruzu.dto.response.MiQrResponse;
import com.almoby.ruralcuruzu.dto.response.SocioActualizadoResponse;
import com.almoby.ruralcuruzu.dto.response.SocioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.SocioResponse;
import com.almoby.ruralcuruzu.dto.response.SocioResumenResponse;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.Rol;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.exception.EmailYaRegistradoException;
import com.almoby.ruralcuruzu.exception.SocioNoEncontradoException;
import com.almoby.ruralcuruzu.repository.SocioRepository;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.security.jwt.QrTokenGenerado;
import com.almoby.ruralcuruzu.security.jwt.QrTokenService;
import com.almoby.ruralcuruzu.service.CuentaAccesoService;
import com.almoby.ruralcuruzu.service.EmailService;
import com.almoby.ruralcuruzu.service.EstadoQrService;
import com.almoby.ruralcuruzu.service.SecuenciaService;
import com.almoby.ruralcuruzu.service.SocioService;
import com.almoby.ruralcuruzu.util.RepositorioUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Ver documento, sección 8.4 ("Aprobación"). Al aprobar una solicitud:
 * 1-3. Se crea el Socio, con número y categoría asignados.
 * 4-6. Se crea el Usuario, con contraseña temporal y rol SOCIO (delegado en
 *      CuentaAccesoService, compartido con ComercioService).
 * 7. Se le mandan las credenciales por correo.
 * 8. Se le exige cambiar la contraseña en el primer ingreso (requiereCambioPassword).
 * 9. Se registra el admin responsable (tanto en el Socio como, aparte, en el
 *    historial de la propia SolicitudSocio).
 *
 * Los datos personales del Socio son una COPIA de los de la solicitud: ver
 * decisión explícita de mantenerlos independientes, para que el Socio se
 * pueda editar después sin alterar el registro histórico de la solicitud.
 */
@Slf4j
@Service
public class SocioServiceImpl implements SocioService {

    private final SocioRepository socioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecuenciaService secuenciaService;
    private final CuentaAccesoService cuentaAccesoService;
    private final EmailService emailService;
    private final EstadoQrService estadoQrService;
    private final QrTokenService qrTokenService;

    public SocioServiceImpl(SocioRepository socioRepository,
                             UsuarioRepository usuarioRepository,
                             SecuenciaService secuenciaService,
                             CuentaAccesoService cuentaAccesoService,
                             EmailService emailService,
                             EstadoQrService estadoQrService,
                             QrTokenService qrTokenService) {
        this.socioRepository = socioRepository;
        this.usuarioRepository = usuarioRepository;
        this.secuenciaService = secuenciaService;
        this.cuentaAccesoService = cuentaAccesoService;
        this.emailService = emailService;
        this.estadoQrService = estadoQrService;
        this.qrTokenService = qrTokenService;
    }

    @Override
    public Socio crearSocioDesdeSolicitud(SolicitudSocio solicitud, String adminId, String adminNombre) {
        String numeroSocio = SocioConstantes.PREFIJO_NUMERO_SOCIO
                + String.format("%06d", secuenciaService.siguienteValor(SocioConstantes.NOMBRE_SECUENCIA_NUMERO_SOCIO));
        Instant ahora = Instant.now();

        Socio socio = Socio.builder()
                .numeroSocio(numeroSocio)
                .categoria(solicitud.getCategoriaSolicitada())
                .tipoPersona(solicitud.getTipoPersona())
                .datosPersonaFisica(copiar(solicitud.getDatosPersonaFisica()))
                .datosPersonaJuridica(copiar(solicitud.getDatosPersonaJuridica()))
                .estado(EstadoSocio.ACTIVO)
                .numeroSolicitudOrigen(solicitud.getNumeroSolicitud())
                .adminResponsableAltaId(adminId)
                .adminResponsableAltaNombre(adminNombre)
                .fechaAlta(ahora)
                .fechaActualizacion(ahora)
                .build();
        // Se guarda antes de crear el Usuario porque el Usuario necesita el id
        // del Socio (refId) y Mongo recién lo asigna al persistir.
        socioRepository.save(socio);

        CuentaAccesoService.CuentaTemporalCreada cuenta = cuentaAccesoService.crearCuentaConPasswordTemporal(
                solicitud.getEmail(), socio.nombreParaMostrar(), Rol.SOCIO, socio.getId());

        // Ahora que el Usuario ya tiene id, se completa la referencia inversa.
        socio.setUsuarioId(cuenta.usuario().getId());
        socioRepository.save(socio);

        log.info("Socio creado numeroSocio={} desde solicitud={} (admin={})",
                numeroSocio, solicitud.getNumeroSolicitud(), adminNombre);

        emailService.enviarCorreoCredencialesSocio(
                cuenta.usuario().getEmail(), socio.nombreParaMostrar(), numeroSocio, cuenta.passwordTemporal());

        return socio;
    }

    @Override
    public SocioCreadoResponse crearSocioManual(AltaManualSocioRequest request, String adminId, String adminNombre) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            log.warn("Alta manual de socio rechazada: email ya registrado ({})", email);
            throw new EmailYaRegistradoException();
        }

        String numeroSocio = SocioConstantes.PREFIJO_NUMERO_SOCIO
                + String.format("%06d", secuenciaService.siguienteValor(SocioConstantes.NOMBRE_SECUENCIA_NUMERO_SOCIO));
        Instant ahora = Instant.now();

        Socio socio = Socio.builder()
                .numeroSocio(numeroSocio)
                .categoria(request.categoria())
                .tipoPersona(request.tipoPersona())
                .datosPersonaFisica(request.tipoPersona() == TipoPersona.FISICA ? mapearFisica(request, email) : null)
                .datosPersonaJuridica(request.tipoPersona() == TipoPersona.JURIDICA ? mapearJuridica(request, email) : null)
                .estado(request.estado() != null ? request.estado() : EstadoSocio.ACTIVO)
                .adminResponsableAltaId(adminId)
                .adminResponsableAltaNombre(adminNombre)
                .fechaAlta(ahora)
                .fechaActualizacion(ahora)
                .build();
        // Se guarda antes de crear el Usuario porque el Usuario necesita el id
        // del Socio (refId) y Mongo recién lo asigna al persistir.
        socioRepository.save(socio);

        CuentaAccesoService.CuentaTemporalCreada cuenta = cuentaAccesoService.crearCuentaConPasswordTemporal(
                email, socio.nombreParaMostrar(), Rol.SOCIO, socio.getId());

        // Ahora que el Usuario ya tiene id, se completa la referencia inversa.
        socio.setUsuarioId(cuenta.usuario().getId());
        socioRepository.save(socio);

        log.info("Socio creado manualmente numeroSocio={} (admin={})", numeroSocio, adminNombre);

        emailService.enviarCorreoCredencialesSocio(
                cuenta.usuario().getEmail(), socio.nombreParaMostrar(), numeroSocio, cuenta.passwordTemporal());

        return SocioCreadoResponse.of(SocioResponse.from(socio));
    }

    @Override
    public List<SocioResumenResponse> listarSocios(EstadoSocio estado) {
        List<Socio> socios = estado != null
                ? socioRepository.findByEstado(estado)
                : socioRepository.findAll();

        return socios.stream().map(SocioResumenResponse::from).toList();
    }

    @Override
    public SocioResponse obtenerSocioPorId(String id) {
        return SocioResponse.from(buscarOFallar(id));
    }

    @Override
    public MiQrResponse obtenerMiQr(String socioId) {
        Socio socio = buscarOFallar(socioId);
        EstadoQrResponse estadoQr = estadoQrService.calcularEstado(socio);
        QrTokenGenerado token = qrTokenService.generar(socio.getId());
        return new MiQrResponse(token.token(), token.expiraEn(), socio.getNumeroSocio(), socio.nombreParaMostrar(),
                socio.getCategoria(), estadoQr.estado(), estadoQr.mensaje(), estadoQr.fechaValidez(), estadoQr.ultimoPago());
    }

    @Override
    public CambiarEstadoSocioResponse cambiarEstadoSocio(String id, CambiarEstadoSocioRequest request) {
        Socio socio = buscarOFallar(id);

        socio.setEstado(request.nuevoEstado());
        socio.setFechaActualizacion(Instant.now());
        socioRepository.save(socio);

        log.info("Socio id={} pasó a estado={}", id, request.nuevoEstado());

        return CambiarEstadoSocioResponse.of(id, request.nuevoEstado());
    }

    @Override
    public SocioActualizadoResponse actualizarSocioParcial(String id, ActualizarSocioParcialRequest request) {
        Socio socio = buscarOFallar(id);

        if (request.categoria() != null) {
            socio.setCategoria(request.categoria());
        }
        if (esNoVacio(request.telefono())) {
            setTelefono(socio, request.telefono());
        }
        if (esNoVacio(request.direccion())) {
            setDireccion(socio, request.direccion());
        }
        if (esNoVacio(request.portalPisoDepartamento())) {
            setPortalPisoDepartamento(socio, request.portalPisoDepartamento());
        }
        if (esNoVacio(request.nombreEstablecimiento())) {
            setNombreEstablecimiento(socio, request.nombreEstablecimiento());
        }
        if (esNoVacio(request.direccionEstablecimiento())) {
            setDireccionEstablecimiento(socio, request.direccionEstablecimiento());
        }

        boolean cambioElCorreo = false;
        if (esNoVacio(request.correoElectronico())) {
            String correoNuevo = request.correoElectronico().trim().toLowerCase();
            String correoActual = socio.obtenerEmail();
            if (correoActual == null || !correoActual.equalsIgnoreCase(correoNuevo)) {
                if (usuarioRepository.existsByEmail(correoNuevo)) {
                    log.warn("Edición parcial de socio id={} rechazada: correo ya registrado ({})", id, correoNuevo);
                    throw new EmailYaRegistradoException();
                }
                setCorreoElectronico(socio, correoNuevo);
                cambioElCorreo = true;
            }
        }

        socio.setFechaActualizacion(Instant.now());
        socioRepository.save(socio);

        if (cambioElCorreo && socio.getUsuarioId() != null) {
            String correoFinal = socio.obtenerEmail();
            usuarioRepository.findById(socio.getUsuarioId()).ifPresent(usuario -> {
                usuario.setEmail(correoFinal);
                usuarioRepository.save(usuario);
            });
        }

        log.info("Socio id={} actualizado parcialmente", id);

        return SocioActualizadoResponse.of(SocioResponse.from(socio));
    }

    private boolean esNoVacio(String valor) {
        return valor != null && !valor.isBlank();
    }

    private void setTelefono(Socio socio, String telefono) {
        if (socio.getTipoPersona() == TipoPersona.FISICA) {
            socio.getDatosPersonaFisica().setTelefono(telefono);
        } else {
            socio.getDatosPersonaJuridica().setTelefono(telefono);
        }
    }

    private void setDireccion(Socio socio, String direccion) {
        if (socio.getTipoPersona() == TipoPersona.FISICA) {
            socio.getDatosPersonaFisica().setDireccion(direccion);
        } else {
            socio.getDatosPersonaJuridica().setDireccion(direccion);
        }
    }

    private void setPortalPisoDepartamento(Socio socio, String portalPisoDepartamento) {
        if (socio.getTipoPersona() == TipoPersona.FISICA) {
            socio.getDatosPersonaFisica().setPortalPisoDepartamento(portalPisoDepartamento);
        } else {
            socio.getDatosPersonaJuridica().setPortalPisoDepartamento(portalPisoDepartamento);
        }
    }

    private void setNombreEstablecimiento(Socio socio, String nombreEstablecimiento) {
        if (socio.getTipoPersona() == TipoPersona.FISICA) {
            socio.getDatosPersonaFisica().setNombreEstablecimiento(nombreEstablecimiento);
        } else {
            socio.getDatosPersonaJuridica().setNombreEstablecimiento(nombreEstablecimiento);
        }
    }

    private void setDireccionEstablecimiento(Socio socio, String direccionEstablecimiento) {
        if (socio.getTipoPersona() == TipoPersona.FISICA) {
            socio.getDatosPersonaFisica().setDireccionEstablecimiento(direccionEstablecimiento);
        } else {
            socio.getDatosPersonaJuridica().setDireccionEstablecimiento(direccionEstablecimiento);
        }
    }

    private void setCorreoElectronico(Socio socio, String correoElectronico) {
        if (socio.getTipoPersona() == TipoPersona.FISICA) {
            socio.getDatosPersonaFisica().setCorreoElectronico(correoElectronico);
        } else {
            socio.getDatosPersonaJuridica().setCorreoElectronico(correoElectronico);
        }
    }

    private Socio buscarOFallar(String id) {
        return RepositorioUtil.buscarOFallar(socioRepository::findById, id, SocioNoEncontradoException::new);
    }

    private DatosPersonaFisica copiar(DatosPersonaFisica original) {
        if (original == null) {
            return null;
        }
        return new DatosPersonaFisica(original.getApellidoYNombre(), original.getDni(),
                original.getFechaNacimiento(), original.getCuitCuil(), original.getDireccion(),
                original.getPortalPisoDepartamento(), original.getTelefono(), original.getCorreoElectronico(),
                original.getNombreEstablecimiento(), original.getDireccionEstablecimiento());
    }

    private DatosPersonaJuridica copiar(DatosPersonaJuridica original) {
        if (original == null) {
            return null;
        }
        return new DatosPersonaJuridica(original.getRazonSocial(), original.getCuit(), original.getDireccion(),
                original.getPortalPisoDepartamento(), original.getTelefono(), original.getCorreoElectronico(),
                original.getNombreEstablecimiento(), original.getNombreResponsable(), original.getDniResponsable(),
                original.getDireccionEstablecimiento());
    }

    private DatosPersonaFisica mapearFisica(AltaManualSocioRequest r, String email) {
        return new DatosPersonaFisica(r.apellidoYNombre(), r.documento(), r.fechaNacimiento(), r.cuit(),
                r.direccion(), r.portalPisoDepartamento(), r.telefono(), email, r.nombreEstablecimiento(),
                r.direccionEstablecimiento());
    }

    private DatosPersonaJuridica mapearJuridica(AltaManualSocioRequest r, String email) {
        return new DatosPersonaJuridica(r.apellidoYNombre(), r.cuit(), r.direccion(), r.portalPisoDepartamento(),
                r.telefono(), email, r.nombreEstablecimiento(), r.nombreResponsable(), r.dniResponsable(),
                r.direccionEstablecimiento());
    }
}
