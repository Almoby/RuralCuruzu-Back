package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
import com.almoby.ruralcuruzu.dto.request.AltaManualSocioRequest;
import com.almoby.ruralcuruzu.dto.response.EstadoQrResponse;
import com.almoby.ruralcuruzu.dto.response.MiQrResponse;
import com.almoby.ruralcuruzu.dto.response.SocioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.SocioResponse;
import com.almoby.ruralcuruzu.dto.response.SocioResumenResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoQr;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.EstadoSolicitud;
import com.almoby.ruralcuruzu.enums.EstadoUsuario;
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
    private UsuarioRepository usuarioRepository;
    @Mock
    private SecuenciaService secuenciaService;
    @Mock
    private CuentaAccesoService cuentaAccesoService;
    @Mock
    private EmailService emailService;
    @Mock
    private EstadoQrService estadoQrService;
    @Mock
    private QrTokenService qrTokenService;

    private SocioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SocioServiceImpl(socioRepository, usuarioRepository, secuenciaService, cuentaAccesoService,
                emailService, estadoQrService, qrTokenService);
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
                "García, Juan Carlos", "28345678", LocalDate.of(1985, 4, 12), "20-28345678-2",
                "Calle 123", "Depto B", "+54 9 3777123456", "juan.garcia@example.com", "Farmacia Central", "Ruta 123 km 4"));
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
        assertThat(socio.getDatosPersonaFisica().getApellidoYNombre()).isEqualTo("García, Juan Carlos");
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

    private AltaManualSocioRequest altaManualFisica(String email, EstadoSocio estado) {
        return new AltaManualSocioRequest(
                CategoriaSocio.ACTIVO, TipoPersona.FISICA, "García, Juan Carlos", "28345678", "20-28345678-2",
                LocalDate.of(1985, 4, 12), "Calle 123", "Depto B", "+54 9 3777123456", email, "Farmacia Central",
                "Ruta 123 km 4", null, null, estado);
    }

    private AltaManualSocioRequest altaManualJuridica(String email) {
        return new AltaManualSocioRequest(
                CategoriaSocio.ADHERENTE, TipoPersona.JURIDICA, "Comercial del Sur S.A.", null, "30-71234567-9",
                null, "Calle 123", null, "+54 9 3777123456", email, "Depósito", "Ruta 123 km 4",
                "María Fernández", "30123456", null);
    }

    @Test
    void crearSocioManual_personaFisica_asignaNumeroYCopiaLosDatos() {
        AltaManualSocioRequest request = altaManualFisica("juan.garcia@example.com", null);

        when(usuarioRepository.existsByEmail("juan.garcia@example.com")).thenReturn(false);
        when(secuenciaService.siguienteValor(SocioConstantes.NOMBRE_SECUENCIA_NUMERO_SOCIO)).thenReturn(7L);
        when(cuentaAccesoService.crearCuentaConPasswordTemporal(anyString(), anyString(), any(Rol.class), anyString()))
                .thenReturn(cuentaFalsa("juan.garcia@example.com"));
        doAnswerAsignarIdSocio();

        SocioCreadoResponse response = service.crearSocioManual(request, "admin-1", "Admin Uno");

        assertThat(response.mensaje()).isEqualTo("Socio dado de alta con éxito");
        assertThat(response.socio().numeroSocio()).isEqualTo("SOC-000007");
        assertThat(response.socio().categoria()).isEqualTo(CategoriaSocio.ACTIVO);
        assertThat(response.socio().tipoPersona()).isEqualTo(TipoPersona.FISICA);
        assertThat(response.socio().estado()).isEqualTo(EstadoSocio.ACTIVO); // sin estado explícito, default ACTIVO
        assertThat(response.socio().datosPersonaFisica().getApellidoYNombre()).isEqualTo("García, Juan Carlos");
        assertThat(response.socio().datosPersonaFisica().getCorreoElectronico()).isEqualTo("juan.garcia@example.com");
    }

    @Test
    void crearSocioManual_conEstadoExplicito_respetaElEstadoElegido() {
        AltaManualSocioRequest request = altaManualFisica("juan.garcia@example.com", EstadoSocio.INACTIVO);

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(secuenciaService.siguienteValor(anyString())).thenReturn(1L);
        when(cuentaAccesoService.crearCuentaConPasswordTemporal(anyString(), anyString(), any(Rol.class), anyString()))
                .thenReturn(cuentaFalsa("juan.garcia@example.com"));
        doAnswerAsignarIdSocio();

        SocioCreadoResponse response = service.crearSocioManual(request, "admin-1", "Admin Uno");

        assertThat(response.socio().estado()).isEqualTo(EstadoSocio.INACTIVO);
    }

    @Test
    void crearSocioManual_personaJuridica_mapeaRazonSocialYResponsable() {
        AltaManualSocioRequest request = altaManualJuridica("contacto@comercialdelsur.com");

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(secuenciaService.siguienteValor(anyString())).thenReturn(1L);
        when(cuentaAccesoService.crearCuentaConPasswordTemporal(anyString(), anyString(), any(Rol.class), anyString()))
                .thenReturn(cuentaFalsa("contacto@comercialdelsur.com"));
        doAnswerAsignarIdSocio();

        SocioCreadoResponse response = service.crearSocioManual(request, "admin-1", "Admin Uno");

        assertThat(response.socio().tipoPersona()).isEqualTo(TipoPersona.JURIDICA);
        assertThat(response.socio().datosPersonaJuridica().getRazonSocial()).isEqualTo("Comercial del Sur S.A.");
        assertThat(response.socio().datosPersonaJuridica().getNombreResponsable()).isEqualTo("María Fernández");
        assertThat(response.socio().datosPersonaFisica()).isNull();
    }

    @Test
    void crearSocioManual_delegaLaCreacionDeUsuarioConRolSocio() {
        AltaManualSocioRequest request = altaManualFisica("juan.garcia@example.com", null);

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(secuenciaService.siguienteValor(anyString())).thenReturn(1L);
        when(cuentaAccesoService.crearCuentaConPasswordTemporal(anyString(), anyString(), any(Rol.class), anyString()))
                .thenReturn(cuentaFalsa("juan.garcia@example.com"));
        doAnswerAsignarIdSocio();

        SocioCreadoResponse response = service.crearSocioManual(request, "admin-1", "Admin Uno");

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Rol> rolCaptor = ArgumentCaptor.forClass(Rol.class);
        verify(cuentaAccesoService).crearCuentaConPasswordTemporal(
                emailCaptor.capture(), anyString(), rolCaptor.capture(), anyString());

        assertThat(emailCaptor.getValue()).isEqualTo("juan.garcia@example.com");
        assertThat(rolCaptor.getValue()).isEqualTo(Rol.SOCIO);
        verify(emailService).enviarCorreoCredencialesSocio(
                eq("juan.garcia@example.com"), eq("García, Juan Carlos"), eq(response.socio().numeroSocio()), eq("PasswordTemp1"));
    }

    @Test
    void crearSocioManual_emailYaRegistrado_lanzaExcepcionYNoCreaNada() {
        AltaManualSocioRequest request = altaManualFisica("ya.registrado@example.com", null);
        when(usuarioRepository.existsByEmail("ya.registrado@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.crearSocioManual(request, "admin-1", "Admin Uno"))
                .isInstanceOf(EmailYaRegistradoException.class);

        verify(socioRepository, never()).save(any(Socio.class));
    }

    private Socio socioActivo(String id, String numeroSocio, String nombre) {
        return Socio.builder()
                .id(id)
                .numeroSocio(numeroSocio)
                .categoria(CategoriaSocio.ACTIVO)
                .tipoPersona(TipoPersona.FISICA)
                .datosPersonaFisica(new DatosPersonaFisica(
                        nombre, "28345678", LocalDate.of(1985, 4, 12), "20-28345678-2",
                        "Calle 123", "Depto B", "+54 9 3777123456", "socio@example.com", "Farmacia Central",
                        "Ruta 123 km 4"))
                .estado(EstadoSocio.ACTIVO)
                .build();
    }

    @Test
    void listarSocios_sinFiltro_devuelveTodos() {
        when(socioRepository.findAll()).thenReturn(List.of(
                socioActivo("socio-1", "SOC-000001", "García, Juan Carlos"),
                socioActivo("socio-2", "SOC-000002", "Pérez, Ana")));

        List<SocioResumenResponse> resultado = service.listarSocios(null);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).numeroSocio()).isEqualTo("SOC-000001");
        assertThat(resultado.get(0).nombre()).isEqualTo("García, Juan Carlos");
        assertThat(resultado.get(1).numeroSocio()).isEqualTo("SOC-000002");
    }

    @Test
    void listarSocios_conFiltroDeEstado_delegaEnElRepositorio() {
        when(socioRepository.findByEstado(EstadoSocio.INACTIVO)).thenReturn(
                List.of(socioActivo("socio-3", "SOC-000003", "López, Marta")));

        List<SocioResumenResponse> resultado = service.listarSocios(EstadoSocio.INACTIVO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).numeroSocio()).isEqualTo("SOC-000003");
    }

    @Test
    void obtenerSocioPorId_existente_devuelveElDetalle() {
        when(socioRepository.findById("socio-1")).thenReturn(
                Optional.of(socioActivo("socio-1", "SOC-000001", "García, Juan Carlos")));

        SocioResponse resultado = service.obtenerSocioPorId("socio-1");

        assertThat(resultado.id()).isEqualTo("socio-1");
        assertThat(resultado.numeroSocio()).isEqualTo("SOC-000001");
        assertThat(resultado.nombre()).isEqualTo("García, Juan Carlos");
        assertThat(resultado.categoria()).isEqualTo(CategoriaSocio.ACTIVO);
    }

    @Test
    void obtenerSocioPorId_inexistente_lanzaExcepcion() {
        when(socioRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerSocioPorId("no-existe"))
                .isInstanceOf(SocioNoEncontradoException.class);
    }

    @Test
    void obtenerMiQr_devuelveElTokenGeneradoYSuEstadoQr() {
        Socio socio = socioActivo("socio-1", "SOC-000001", "García, Juan Carlos");
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio));
        LocalDate vencimiento = LocalDate.of(2026, 6, 9);
        Instant ultimoPago = Instant.parse("2026-05-09T00:00:00Z");
        when(estadoQrService.calcularEstado(socio)).thenReturn(
                new EstadoQrResponse(EstadoQr.ACTIVO, "Tu QR está activo.", vencimiento, ultimoPago));
        Instant expiraEn = Instant.parse("2026-07-27T20:31:00Z");
        when(qrTokenService.generar("socio-1")).thenReturn(new QrTokenGenerado("token-jwt-abc", expiraEn));

        MiQrResponse resultado = service.obtenerMiQr("socio-1");

        assertThat(resultado.token()).isEqualTo("token-jwt-abc");
        assertThat(resultado.expiraEn()).isEqualTo(expiraEn);
        assertThat(resultado.numeroSocio()).isEqualTo("SOC-000001");
        assertThat(resultado.nombre()).isEqualTo("García, Juan Carlos");
        assertThat(resultado.categoria()).isEqualTo(CategoriaSocio.ACTIVO);
        assertThat(resultado.estado()).isEqualTo(EstadoQr.ACTIVO);
        assertThat(resultado.mensaje()).isEqualTo("Tu QR está activo.");
        assertThat(resultado.fechaValidez()).isEqualTo(vencimiento);
        assertThat(resultado.ultimoPago()).isEqualTo(ultimoPago);
    }

    @Test
    void obtenerMiQr_generaUnTokenNuevoEnCadaLlamada() {
        Socio socio = socioActivo("socio-1", "SOC-000001", "García, Juan Carlos");
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio));
        when(estadoQrService.calcularEstado(socio)).thenReturn(
                new EstadoQrResponse(EstadoQr.ACTIVO, "Tu QR está activo.", null, null));
        when(qrTokenService.generar("socio-1"))
                .thenReturn(new QrTokenGenerado("token-1", Instant.parse("2026-07-27T20:31:00Z")))
                .thenReturn(new QrTokenGenerado("token-2", Instant.parse("2026-07-27T20:32:00Z")));

        MiQrResponse primero = service.obtenerMiQr("socio-1");
        MiQrResponse segundo = service.obtenerMiQr("socio-1");

        assertThat(primero.token()).isEqualTo("token-1");
        assertThat(segundo.token()).isEqualTo("token-2");
        verify(qrTokenService, times(2)).generar("socio-1");
    }

    @Test
    void obtenerMiQr_socioInexistente_lanzaExcepcion() {
        when(socioRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerMiQr("no-existe"))
                .isInstanceOf(SocioNoEncontradoException.class);
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
