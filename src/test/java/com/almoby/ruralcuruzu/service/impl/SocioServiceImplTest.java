package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.almoby.ruralcuruzu.constantes.SocioConstantes;
import com.almoby.ruralcuruzu.domain.DatosPersonaFisica;
import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.domain.SolicitudSocio;
import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.EstadoSolicitud;
import com.almoby.ruralcuruzu.enums.EstadoUsuario;
import com.almoby.ruralcuruzu.enums.Rol;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.repository.SocioRepository;
import com.almoby.ruralcuruzu.service.CuentaAccesoService;
import com.almoby.ruralcuruzu.service.EmailService;
import com.almoby.ruralcuruzu.service.SecuenciaService;

/**
 * Tests unitarios de la creación de un Socio a partir de una solicitud
 * aprobada (documento, sección 8.4). Todas las dependencias van mockeadas.
 * La creación de la cuenta de acceso en sí (Usuario + contraseña temporal)
 * está delegada en CuentaAccesoService, que tiene sus propios tests
 * (CuentaAccesoServiceImplTest); acá solo se verifica que SocioServiceImpl
 * lo llame con los datos correctos.
 */
@ExtendWith(MockitoExtension.class)
class SocioServiceImplTest {

    @Mock
    private SocioRepository socioRepository;
    @Mock
    private SecuenciaService secuenciaService;
    @Mock
    private CuentaAccesoService cuentaAccesoService;
    @Mock
    private EmailService emailService;

    private SocioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SocioServiceImpl(socioRepository, secuenciaService, cuentaAccesoService, emailService);
    }

    private SolicitudSocio solicitudAprobada() {
        SolicitudSocio solicitud = SolicitudSocio.builder()
                .id("sol-1")
                .numeroSolicitud("SOL-000001")
                .categoriaSolicitada(CategoriaSocio.ACTIVO)
                .tipoPersona(TipoPersona.FISICA)
                .email("juan.garcia@example.com")
                .documentos(java.util.List.of("28345678", "20283456782"))
                .estado(EstadoSolicitud.APROBADA)
                .build();
        solicitud.setDatosPersonaFisica(new DatosPersonaFisica(
                "Juan Carlos", "García", "28345678", LocalDate.of(1985, 4, 12), "20-28345678-2",
                "Calle 123", "Depto B", "+54 9 3777123456", "juan.garcia@example.com", "Comerciante", null, null));
        return solicitud;
    }

    private CuentaAccesoService.CuentaTemporalCreada cuentaFalsa(String email) {
        Usuario usuario = Usuario.builder()
                .id("usuario-generado-1")
                .email(email)
                .passwordHash("hash-encriptado")
                .rol(Rol.SOCIO)
                .estado(EstadoUsuario.ACTIVO)
                .requiereCambioPassword(true)
                .build();
        return new CuentaAccesoService.CuentaTemporalCreada(usuario, "PasswordTemp1");
    }

    @Test
    void crearSocioDesdeSolicitud_asignaNumeroYCategoriaYCopiaLosDatos() {
        SolicitudSocio solicitud = solicitudAprobada();

        when(secuenciaService.siguienteValor(SocioConstantes.NOMBRE_SECUENCIA_NUMERO_SOCIO)).thenReturn(7L);
        when(cuentaAccesoService.crearCuentaConPasswordTemporal(anyString(), anyString(), any(Rol.class), anyString()))
                .thenReturn(cuentaFalsa("juan.garcia@example.com"));
        // Mongo asigna el id recién al guardar: se simula completando el id acá.
        doAnswerAsignarIdSocio();

        Socio socio = service.crearSocioDesdeSolicitud(solicitud, "admin-1", "Admin Uno");

        assertThat(socio.getNumeroSocio()).isEqualTo("SOC-000007");
        assertThat(socio.getCategoria()).isEqualTo(CategoriaSocio.ACTIVO);
        assertThat(socio.getTipoPersona()).isEqualTo(TipoPersona.FISICA);
        assertThat(socio.getEstado()).isEqualTo(EstadoSocio.ACTIVO);
        assertThat(socio.getNumeroSolicitudOrigen()).isEqualTo("SOL-000001");
        assertThat(socio.getAdminResponsableAltaId()).isEqualTo("admin-1");
        assertThat(socio.getAdminResponsableAltaNombre()).isEqualTo("Admin Uno");

        // Copia, no la misma instancia: editar el Socio después no debe tocar la solicitud.
        assertThat(socio.getDatosPersonaFisica()).isNotSameAs(solicitud.getDatosPersonaFisica());
        assertThat(socio.getDatosPersonaFisica().getNombre()).isEqualTo("Juan Carlos");
        assertThat(socio.getDatosPersonaFisica().getCorreoElectronico()).isEqualTo("juan.garcia@example.com");
    }

    @Test
    void crearSocioDesdeSolicitud_delegaLaCreacionDeUsuarioConRolSocio() {
        SolicitudSocio solicitud = solicitudAprobada();

        when(secuenciaService.siguienteValor(anyString())).thenReturn(1L);
        when(cuentaAccesoService.crearCuentaConPasswordTemporal(anyString(), anyString(), any(Rol.class), anyString()))
                .thenReturn(cuentaFalsa("juan.garcia@example.com"));
        doAnswerAsignarIdSocio();

        Socio socio = service.crearSocioDesdeSolicitud(solicitud, "admin-1", "Admin Uno");

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nombreCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Rol> rolCaptor = ArgumentCaptor.forClass(Rol.class);
        ArgumentCaptor<String> refIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(cuentaAccesoService).crearCuentaConPasswordTemporal(
                emailCaptor.capture(), nombreCaptor.capture(), rolCaptor.capture(), refIdCaptor.capture());

        assertThat(emailCaptor.getValue()).isEqualTo("juan.garcia@example.com");
        assertThat(nombreCaptor.getValue()).isEqualTo("García, Juan Carlos");
        assertThat(rolCaptor.getValue()).isEqualTo(Rol.SOCIO);
        assertThat(refIdCaptor.getValue()).isEqualTo(socio.getId());
    }

    @Test
    void crearSocioDesdeSolicitud_guardaElSocioDosVecesParaCompletarElUsuarioId() {
        SolicitudSocio solicitud = solicitudAprobada();

        when(secuenciaService.siguienteValor(anyString())).thenReturn(1L);
        when(cuentaAccesoService.crearCuentaConPasswordTemporal(anyString(), anyString(), any(Rol.class), anyString()))
                .thenReturn(cuentaFalsa("juan.garcia@example.com"));
        doAnswerAsignarIdSocio();

        Socio socio = service.crearSocioDesdeSolicitud(solicitud, "admin-1", "Admin Uno");

        assertThat(socio.getUsuarioId()).isEqualTo("usuario-generado-1");
        verify(socioRepository, times(2)).save(any(Socio.class));
    }

    @Test
    void crearSocioDesdeSolicitud_mandaElCorreoDeCredenciales() {
        SolicitudSocio solicitud = solicitudAprobada();

        when(secuenciaService.siguienteValor(anyString())).thenReturn(42L);
        when(cuentaAccesoService.crearCuentaConPasswordTemporal(anyString(), anyString(), any(Rol.class), anyString()))
                .thenReturn(cuentaFalsa("juan.garcia@example.com"));
        doAnswerAsignarIdSocio();

        service.crearSocioDesdeSolicitud(solicitud, "admin-1", "Admin Uno");

        verify(emailService).enviarCorreoCredencialesSocio(
                eq("juan.garcia@example.com"), eq("García, Juan Carlos"), eq("SOC-000042"), eq("PasswordTemp1"));
    }

    private void doAnswerAsignarIdSocio() {
        when(socioRepository.save(any(Socio.class))).thenAnswer(invocation -> {
            Socio socio = invocation.getArgument(0);
            if (socio.getId() == null) {
                socio.setId("socio-generado-1");
            }
            return socio;
        });
    }
}
