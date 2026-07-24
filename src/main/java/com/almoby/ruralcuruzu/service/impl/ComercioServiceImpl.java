package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.Comercio;
import com.almoby.ruralcuruzu.dto.request.AltaComercioRequest;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoComercioRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoComercioResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioResumenResponse;
import com.almoby.ruralcuruzu.enums.EstadoComercio;
import com.almoby.ruralcuruzu.enums.Rol;
import com.almoby.ruralcuruzu.exception.ComercioNoEncontradoException;
import com.almoby.ruralcuruzu.exception.CorreoYaRegistradoException;
import com.almoby.ruralcuruzu.exception.CuitYaRegistradoException;
import com.almoby.ruralcuruzu.repository.ComercioRepository;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.service.ComercioService;
import com.almoby.ruralcuruzu.service.CuentaAccesoService;
import com.almoby.ruralcuruzu.service.EmailService;

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
 * promociones (esto último se resuelve cuando exista el módulo de Beneficios).
 */
@Slf4j
@Service
public class ComercioServiceImpl implements ComercioService {

    private final ComercioRepository comercioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaAccesoService cuentaAccesoService;
    private final EmailService emailService;

    public ComercioServiceImpl(ComercioRepository comercioRepository,
                                UsuarioRepository usuarioRepository,
                                CuentaAccesoService cuentaAccesoService,
                                EmailService emailService) {
        this.comercioRepository = comercioRepository;
        this.usuarioRepository = usuarioRepository;
        this.cuentaAccesoService = cuentaAccesoService;
        this.emailService = emailService;
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

        return ComercioCreadoResponse.of(ComercioResponse.from(comercio));
    }

    @Override
    public List<ComercioResumenResponse> listarComercios(EstadoComercio estado) {
        List<Comercio> comercios = estado != null
                ? comercioRepository.findByEstado(estado)
                : comercioRepository.findAll();

        return comercios.stream().map(ComercioResumenResponse::from).toList();
    }

    @Override
    public ComercioResponse obtenerComercioPorId(String id) {
        return ComercioResponse.from(buscarOFallar(id));
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

    private Comercio buscarOFallar(String id) {
        return comercioRepository.findById(id)
                .orElseThrow(() -> new ComercioNoEncontradoException(id));
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
