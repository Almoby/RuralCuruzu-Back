package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.constantes.SocioConstantes;
import com.almoby.ruralcuruzu.domain.DatosPersonaFisica;
import com.almoby.ruralcuruzu.domain.DatosPersonaJuridica;
import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.domain.SolicitudSocio;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.Rol;
import com.almoby.ruralcuruzu.repository.SocioRepository;
import com.almoby.ruralcuruzu.service.CuentaAccesoService;
import com.almoby.ruralcuruzu.service.EmailService;
import com.almoby.ruralcuruzu.service.SecuenciaService;
import com.almoby.ruralcuruzu.service.SocioService;

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
    private final SecuenciaService secuenciaService;
    private final CuentaAccesoService cuentaAccesoService;
    private final EmailService emailService;

    public SocioServiceImpl(SocioRepository socioRepository,
                             SecuenciaService secuenciaService,
                             CuentaAccesoService cuentaAccesoService,
                             EmailService emailService) {
        this.socioRepository = socioRepository;
        this.secuenciaService = secuenciaService;
        this.cuentaAccesoService = cuentaAccesoService;
        this.emailService = emailService;
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

    private DatosPersonaFisica copiar(DatosPersonaFisica original) {
        if (original == null) {
            return null;
        }
        return new DatosPersonaFisica(original.getNombre(), original.getApellido(), original.getDni(),
                original.getFechaNacimiento(), original.getCuitCuil(), original.getDireccion(),
                original.getPortalPisoDepartamento(), original.getTelefono(), original.getCorreoElectronico(),
                original.getOcupacion(), original.getNombreEstablecimiento(), original.getDireccionEstablecimiento());
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
}
