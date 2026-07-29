package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.domain.Comercio;
import com.almoby.ruralcuruzu.domain.ComercioEliminado;
import com.almoby.ruralcuruzu.domain.HistorialBeneficio;
import com.almoby.ruralcuruzu.dto.request.ActualizarComercioParcialRequest;
import com.almoby.ruralcuruzu.dto.request.AltaComercioRequest;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoComercioRequest;
import com.almoby.ruralcuruzu.dto.request.EliminarComercioRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoComercioResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioActualizadoResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioEliminadoResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioResponse.PromocionResumenResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioResumenResponse;
import com.almoby.ruralcuruzu.dto.response.EliminarComercioResponse;
import com.almoby.ruralcuruzu.enums.EstadoComercio;
import com.almoby.ruralcuruzu.enums.EstadoUsoBeneficio;
import com.almoby.ruralcuruzu.enums.Rol;
import com.almoby.ruralcuruzu.exception.ComercioNoEncontradoException;
import com.almoby.ruralcuruzu.exception.CorreoYaRegistradoException;
import com.almoby.ruralcuruzu.exception.CuitYaRegistradoException;
import com.almoby.ruralcuruzu.repository.BeneficioRepository;
import com.almoby.ruralcuruzu.repository.ComercioEliminadoRepository;
import com.almoby.ruralcuruzu.repository.ComercioRepository;
import com.almoby.ruralcuruzu.repository.HistorialBeneficioRepository;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.service.ComercioService;
import com.almoby.ruralcuruzu.service.CuentaAccesoService;
import com.almoby.ruralcuruzu.service.EmailService;
import com.almoby.ruralcuruzu.util.RepositorioUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Ver documento, sección 12 ("Comercios adheridos desde el administrador").
 * A diferencia de Socio, no hay un formulario público de "solicitud": el
 * admin carga el comercio directamente (12.2), y siempre se crea también su
 * Usuario con contraseña temporal y rol COMERCIO (12.3) — mismo mecanismo
 * que Socio, vía CuentaAccesoService.
 *
 * Los estados (12.4: ACTIVO, INACTIVO, SUSPENDIDO, DADO_DE_BAJA) no tienen un
 * grafo de transiciones restringido como SolicitudSocio: el documento no
 * exige ninguna regla puntual acá, así que el admin puede pasar de cualquier
 * estado a cualquier otro libremente. Cuando no está ACTIVO, no puede
 * loguearse (ver AuthServiceImpl.login), no puede validar QR ni mostrar sus
 * promociones.
 *
 * "Eliminar" (eliminarComercio) es distinto de "cambiar estado a
 * DADO_DE_BAJA": es un borrado físico real, a pedido explícito del dueño del
 * proyecto. Antes de borrar nada se guarda una copia de auditoría
 * (ComercioEliminado), y se cascadea el borrado de sus Beneficios y de su
 * cuenta de acceso (Usuario) para no dejar nada huérfano ni que pueda seguir
 * logueándose. El HistorialBeneficio (comprobantes de uso) NO se toca: ya
 * tiene todos sus datos denormalizados, así que sigue teniendo sentido aunque
 * el Comercio y el Beneficio originales ya no existan.
 */
@Slf4j
@Service
public class ComercioServiceImpl implements ComercioService {

    private final ComercioRepository comercioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaAccesoService cuentaAccesoService;
    private final EmailService emailService;
    private final BeneficioRepository beneficioRepository;
    private final HistorialBeneficioRepository historialBeneficioRepository;
    private final ComercioEliminadoRepository comercioEliminadoRepository;

    public ComercioServiceImpl(ComercioRepository comercioRepository,
                                UsuarioRepository usuarioRepository,
                                CuentaAccesoService cuentaAccesoService,
                                EmailService emailService,
                                BeneficioRepository beneficioRepository,
                                HistorialBeneficioRepository historialBeneficioRepository,
                                ComercioEliminadoRepository comercioEliminadoRepository) {
        this.comercioRepository = comercioRepository;
        this.usuarioRepository = usuarioRepository;
        this.cuentaAccesoService = cuentaAccesoService;
        this.emailService = emailService;
        this.beneficioRepository = beneficioRepository;
        this.historialBeneficioRepository = historialBeneficioRepository;
        this.comercioEliminadoRepository = comercioEliminadoRepository;
    }

    @Override
    public ComercioCreadoResponse crearComercio(AltaComercioRequest request, String adminId, String adminNombre) {
        String correo = request.correoElectronico().trim().toLowerCase();
        validarNoDuplicado(request.cuit(), correo);

        Instant ahora = Instant.now();
        Comercio comercio = Comercio.builder()
                .nombreComercial(request.nombreComercial())
                .razonSocial(request.razonSocial())
                .cuit(request.cuit())
                .rubro(request.rubro())
                .telefono(request.telefono())
                .correoElectronico(correo)
                .direccion(request.direccion())
                .logo(request.logo())
                .descripcion(request.descripcion())
                .estado(request.estado() != null ? request.estado() : EstadoComercio.ACTIVO)
                .adminResponsableAltaId(adminId)
                .adminResponsableAltaNombre(adminNombre)
                .fechaAlta(ahora)
                .fechaActualizacion(ahora)
                .build();
        // Se guarda antes de crear el Usuario porque el Usuario necesita el id
        // del Comercio (refId) y Mongo recién lo asigna al persistir.
        comercioRepository.save(comercio);

        CuentaAccesoService.CuentaTemporalCreada cuenta = cuentaAccesoService.crearCuentaConPasswordTemporal(
                correo, comercio.getNombreComercial(), Rol.COMERCIO, comercio.getId());

        comercio.setUsuarioId(cuenta.usuario().getId());
        comercioRepository.save(comercio);

        log.info("Comercio creado id={} cuit={} (admin={})", comercio.getId(), comercio.getCuit(), adminNombre);

        emailService.enviarCorreoCredencialesComercio(
                cuenta.usuario().getEmail(), comercio.getNombreComercial(), cuenta.passwordTemporal());

        return ComercioCreadoResponse.of(ComercioResponse.from(comercio, List.of()));
    }

    @Override
    public List<ComercioResumenResponse> listarComercios(EstadoComercio estado) {
        List<Comercio> comercios = estado != null
                ? comercioRepository.findByEstado(estado)
                : comercioRepository.findAll();

        // Se traen TODOS los beneficios e historiales de una sola vez y se
        // agrupan acá en vez de consultar por comercio dentro del map: evita
        // N+1 consultas cuando el listado tiene muchos comercios (mismo
        // criterio que DashboardServiceImpl).
        Map<String, Long> cantidadPromocionesPorComercio = beneficioRepository.findAll().stream()
                .collect(Collectors.groupingBy(Beneficio::getComercioId, Collectors.counting()));
        Map<String, Long> consumosPorComercio = historialBeneficioRepository.findAll().stream()
                .filter(h -> h.getEstado() == EstadoUsoBeneficio.USADO)
                .collect(Collectors.groupingBy(HistorialBeneficio::getComercioId, Collectors.counting()));

        return comercios.stream()
                .map(comercio -> ComercioResumenResponse.from(
                        comercio,
                        cantidadPromocionesPorComercio.getOrDefault(comercio.getId(), 0L),
                        consumosPorComercio.getOrDefault(comercio.getId(), 0L)))
                .toList();
    }

    @Override
    public ComercioResponse obtenerComercioPorId(String id) {
        Comercio comercio = buscarOFallar(id);
        return ComercioResponse.from(comercio, construirPromociones(id));
    }

    /** Promociones de un comercio con su cantidad de usos en el mes en curso (documento, ficha de comercio). */
    private List<PromocionResumenResponse> construirPromociones(String comercioId) {
        List<Beneficio> beneficios = beneficioRepository.findByComercioId(comercioId);
        List<HistorialBeneficio> historial = historialBeneficioRepository.findByComercioId(comercioId);
        Instant inicioMesActual = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Map<String, Long> usosEsteMesPorBeneficio = historial.stream()
                .filter(h -> h.getEstado() == EstadoUsoBeneficio.USADO
                        && h.getFechaUso() != null
                        && !h.getFechaUso().isBefore(inicioMesActual))
                .collect(Collectors.groupingBy(HistorialBeneficio::getBeneficioId, Collectors.counting()));

        return beneficios.stream()
                .map(beneficio -> PromocionResumenResponse.from(
                        beneficio, usosEsteMesPorBeneficio.getOrDefault(beneficio.getId(), 0L)))
                .toList();
    }

    @Override
    public CambiarEstadoComercioResponse cambiarEstadoComercio(String id, CambiarEstadoComercioRequest request) {
        Comercio comercio = buscarOFallar(id);

        comercio.setEstado(request.nuevoEstado());
        comercio.setFechaActualizacion(Instant.now());
        comercioRepository.save(comercio);

        log.info("Comercio id={} pasó a estado={}", id, request.nuevoEstado());

        return CambiarEstadoComercioResponse.of(id, request.nuevoEstado());
    }

    @Override
    public ComercioActualizadoResponse actualizarComercioParcial(String id, ActualizarComercioParcialRequest request) {
        Comercio comercio = buscarOFallar(id);

        if (esNoVacio(request.cuit()) && !comercio.getCuit().equals(request.cuit())) {
            if (comercioRepository.existsByCuitAndIdNot(request.cuit(), id)) {
                log.warn("Edición parcial de comercio id={} rechazada: CUIT ya registrado ({})", id, request.cuit());
                throw new CuitYaRegistradoException();
            }
            comercio.setCuit(request.cuit());
        }

        boolean cambioElCorreo = false;
        if (esNoVacio(request.correoElectronico())) {
            String correoNuevo = request.correoElectronico().trim().toLowerCase();
            if (!comercio.getCorreoElectronico().equalsIgnoreCase(correoNuevo)) {
                boolean correoRegistrado = usuarioRepository.existsByEmail(correoNuevo)
                        || comercioRepository.existsByCorreoElectronicoIgnoreCaseAndIdNot(correoNuevo, id);
                if (correoRegistrado) {
                    log.warn("Edición parcial de comercio id={} rechazada: correo ya registrado ({})", id, correoNuevo);
                    throw new CorreoYaRegistradoException();
                }
                comercio.setCorreoElectronico(correoNuevo);
                cambioElCorreo = true;
            }
        }

        if (esNoVacio(request.nombreComercial())) {
            comercio.setNombreComercial(request.nombreComercial());
        }
        if (esNoVacio(request.razonSocial())) {
            comercio.setRazonSocial(request.razonSocial());
        }
        if (esNoVacio(request.rubro())) {
            comercio.setRubro(request.rubro());
        }
        if (esNoVacio(request.telefono())) {
            comercio.setTelefono(request.telefono());
        }
        if (esNoVacio(request.direccion())) {
            comercio.setDireccion(request.direccion());
        }
        if (esNoVacio(request.logo())) {
            comercio.setLogo(request.logo());
        }
        if (esNoVacio(request.descripcion())) {
            comercio.setDescripcion(request.descripcion());
        }

        comercio.setFechaActualizacion(Instant.now());
        comercioRepository.save(comercio);

        if (cambioElCorreo && comercio.getUsuarioId() != null) {
            String correoFinal = comercio.getCorreoElectronico();
            usuarioRepository.findById(comercio.getUsuarioId()).ifPresent(usuario -> {
                usuario.setEmail(correoFinal);
                usuarioRepository.save(usuario);
            });
        }

        log.info("Comercio id={} actualizado parcialmente", id);

        return ComercioActualizadoResponse.of(ComercioResponse.from(comercio, construirPromociones(id)));
    }

    private boolean esNoVacio(String valor) {
        return valor != null && !valor.isBlank();
    }

    @Override
    public EliminarComercioResponse eliminarComercio(String id, EliminarComercioRequest request,
                                                        String adminId, String adminNombre) {
        Comercio comercio = buscarOFallar(id);
        Instant ahora = Instant.now();

        ComercioEliminado tombstone = ComercioEliminado.builder()
                .comercioIdOriginal(comercio.getId())
                .nombreComercial(comercio.getNombreComercial())
                .razonSocial(comercio.getRazonSocial())
                .cuit(comercio.getCuit())
                .rubro(comercio.getRubro())
                .telefono(comercio.getTelefono())
                .correoElectronico(comercio.getCorreoElectronico())
                .direccion(comercio.getDireccion())
                .estadoAlEliminar(comercio.getEstado())
                .fechaAlta(comercio.getFechaAlta())
                .motivo(request.motivo())
                .adminResponsableBajaId(adminId)
                .adminResponsableBajaNombre(adminNombre)
                .fechaBaja(ahora)
                .build();
        // Se guarda ANTES de borrar nada: si algo de lo que sigue fallara, preferimos
        // un registro de auditoría "de más" antes que perder la constancia de la baja.
        comercioEliminadoRepository.save(tombstone);

        // Cascada: los beneficios de este comercio ya no tienen sentido sin él (y
        // el socio-facing los mostraría huérfanos). El HistorialBeneficio NO se
        // toca: ya tiene todo denormalizado, sigue siendo un comprobante válido.
        beneficioRepository.deleteByComercioId(id);

        // La cuenta de acceso se borra para que el comercio no pueda seguir
        // logueándose: a diferencia de "estado", acá no queremos ni siquiera
        // dejarle un Usuario inactivo dando vueltas.
        if (comercio.getUsuarioId() != null) {
            usuarioRepository.deleteById(comercio.getUsuarioId());
        }

        comercioRepository.deleteById(id);

        log.info("Comercio id={} ({}) eliminado físicamente por admin={} motivo={}",
                id, comercio.getCuit(), adminNombre, request.motivo());

        emailService.enviarCorreoComercioEliminado(
                comercio.getCorreoElectronico(), comercio.getNombreComercial(), request.motivo());

        return EliminarComercioResponse.of(ComercioEliminadoResponse.from(tombstone));
    }

    @Override
    public List<ComercioEliminadoResponse> listarComerciosEliminados() {
        return comercioEliminadoRepository.findAllByOrderByFechaBajaDesc().stream()
                .map(ComercioEliminadoResponse::from)
                .toList();
    }

    private Comercio buscarOFallar(String id) {
        return RepositorioUtil.buscarOFallar(comercioRepository::findById, id, ComercioNoEncontradoException::new);
    }

    private void validarNoDuplicado(String cuit, String correo) {
        if (comercioRepository.existsByCuit(cuit)) {
            log.warn("Alta de comercio rechazada: CUIT ya registrado ({})", cuit);
            throw new CuitYaRegistradoException();
        }

        boolean correoRegistrado = usuarioRepository.existsByEmail(correo)
                || comercioRepository.existsByCorreoElectronicoIgnoreCase(correo);
        if (correoRegistrado) {
            log.warn("Alta de comercio rechazada: correo ya registrado ({})", correo);
            throw new CorreoYaRegistradoException();
        }
    }
}
