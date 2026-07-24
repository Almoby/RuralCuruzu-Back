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
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.almoby.ruralcuruzu.domain.Comercio;
import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.enums.EstadoComercio;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.EstadoUsuario;
import com.almoby.ruralcuruzu.enums.Rol;
import com.almoby.ruralcuruzu.dto.request.LoginRequest;
import com.almoby.ruralcuruzu.dto.response.LoginResponse;
import com.almoby.ruralcuruzu.exception.CredencialesInvalidasException;
import com.almoby.ruralcuruzu.exception.CuentaBloqueadaTemporalmenteException;
import com.almoby.ruralcuruzu.exception.PasswordActualIncorrectaException;
import com.almoby.ruralcuruzu.exception.PasswordIgualException;
import com.almoby.ruralcuruzu.exception.UsuarioInactivoException;
import com.almoby.ruralcuruzu.repository.ComercioRepository;
import com.almoby.ruralcuruzu.repository.SocioRepository;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.security.RateLimiterService;
import com.almoby.ruralcuruzu.security.jwt.JwtService;
import com.almoby.ruralcuruzu.service.EmailService;
import com.almoby.ruralcuruzu.service.PasswordResetTokenService;
import com.almoby.ruralcuruzu.service.RefreshTokenService;
import com.almoby.ruralcuruzu.service.TokenRevocadoService;

/**
 * Tests unitarios de la lógica de negocio de AuthServiceImpl. Todas las
 * dependencias van mockeadas: no levantan Spring ni Mongo, así que corren
 * rápido y no dependen de nada externo.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final long DURACION_BLOQUEO_MINUTOS = 15;
    private static final int MAX_SOLICITUDES_RECUPERACION_POR_HORA = 3;

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SocioRepository socioRepository;
    @Mock
    private ComercioRepository comercioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private TokenRevocadoService tokenRevocadoService;
    @Mock
    private PasswordResetTokenService passwordResetTokenService;
    @Mock
    private EmailService emailService;
    @Mock
    private RateLimiterService rateLimiterService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                usuarioRepository,
                socioRepository,
                comercioRepository,
                passwordEncoder,
                jwtService,
                tokenRevocadoService,
                passwordResetTokenService,
                emailService,
                rateLimiterService,
                refreshTokenService,
                MAX_INTENTOS_FALLIDOS,
                DURACION_BLOQUEO_MINUTOS,
                MAX_SOLICITUDES_RECUPERACION_POR_HORA);
    }

    /**
     * Rol SOCIO con refId="socio-1". Los tests que llegan hasta la validación
     * del perfil vinculado deben stubbear socioRepository.findById("socio-1")
     * explícitamente (con un Socio activo, o dejarlo sin stub para simular
     * que no existe / no está activo).
     */
    private Usuario usuarioActivo() {
        Usuario usuario = new Usuario();
        usuario.setId("u1");
        usuario.setEmail("socio@ruralcuruzu.com");
        usuario.setPasswordHash("hash-guardado");
        usuario.setRol(Rol.SOCIO);
        usuario.setRefId("socio-1");
        usuario.setNombre("Juan Pérez");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setRequiereCambioPassword(false);
        usuario.setIntentosFallidos(0);
        return usuario;
    }

    private Socio socioActivo() {
        return Socio.builder().id("socio-1").estado(EstadoSocio.ACTIVO).build();
    }

    // ---------- login ----------

    @Test
    void login_conCredencialesValidas_devuelveTokenYRefreshToken() {
        Usuario usuario = usuarioActivo();
        LoginRequest request = new LoginRequest(usuario.getEmail(), "password-correcta");

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-correcta", usuario.getPasswordHash())).thenReturn(true);
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socioActivo()));
        when(jwtService.generarToken(usuario)).thenReturn("access-token");
        when(jwtService.expiracionEnSegundos()).thenReturn(3600L);
        when(refreshTokenService.generar(usuario.getId())).thenReturn("refresh-token");

        LoginResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.rol()).isEqualTo(Rol.SOCIO);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void login_conEmailInexistente_lanzaCredencialesInvalidas() {
        LoginRequest request = new LoginRequest("no-existe@ruralcuruzu.com", "cualquiera");
        when(usuarioRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void login_conPasswordIncorrecta_lanzaCredencialesInvalidasYRegistraIntento() {
        Usuario usuario = usuarioActivo();
        LoginRequest request = new LoginRequest(usuario.getEmail(), "password-incorrecta");

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-incorrecta", usuario.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThat(usuario.getIntentosFallidos()).isEqualTo(1);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void login_alAlcanzarMaximoIntentosFallidos_bloqueaLaCuentaTemporalmente() {
        Usuario usuario = usuarioActivo();
        usuario.setIntentosFallidos(MAX_INTENTOS_FALLIDOS - 1);
        LoginRequest request = new LoginRequest(usuario.getEmail(), "password-incorrecta");

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-incorrecta", usuario.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isAfter(Instant.now());
    }

    @Test
    void login_conCuentaBloqueadaTemporalmente_lanzaCuentaBloqueada() {
        Usuario usuario = usuarioActivo();
        usuario.setBloqueadoHasta(Instant.now().plus(10, ChronoUnit.MINUTES));
        LoginRequest request = new LoginRequest(usuario.getEmail(), "password-correcta");

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CuentaBloqueadaTemporalmenteException.class);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_conUsuarioInactivo_lanzaUsuarioInactivo() {
        Usuario usuario = usuarioActivo();
        usuario.setEstado(EstadoUsuario.BLOQUEADO);
        LoginRequest request = new LoginRequest(usuario.getEmail(), "password-correcta");

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-correcta", usuario.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UsuarioInactivoException.class);

        verify(jwtService, never()).generarToken(any());
    }

    @Test
    void login_conSocioVinculadoInactivo_lanzaUsuarioInactivo() {
        Usuario usuario = usuarioActivo();
        LoginRequest request = new LoginRequest(usuario.getEmail(), "password-correcta");

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-correcta", usuario.getPasswordHash())).thenReturn(true);
        when(socioRepository.findById("socio-1"))
                .thenReturn(Optional.of(Socio.builder().id("socio-1").estado(EstadoSocio.DADO_DE_BAJA).build()));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UsuarioInactivoException.class);

        verify(jwtService, never()).generarToken(any());
    }

    @Test
    void login_conSocioVinculadoInexistente_lanzaUsuarioInactivo() {
        Usuario usuario = usuarioActivo();
        LoginRequest request = new LoginRequest(usuario.getEmail(), "password-correcta");

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-correcta", usuario.getPasswordHash())).thenReturn(true);
        when(socioRepository.findById("socio-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UsuarioInactivoException.class);
    }

    @Test
    void login_conComercioVinculadoInactivo_lanzaUsuarioInactivo() {
        Usuario usuario = usuarioActivo();
        usuario.setRol(Rol.COMERCIO);
        usuario.setRefId("comercio-1");
        LoginRequest request = new LoginRequest(usuario.getEmail(), "password-correcta");

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-correcta", usuario.getPasswordHash())).thenReturn(true);
        when(comercioRepository.findById("comercio-1"))
                .thenReturn(Optional.of(Comercio.builder().id("comercio-1").estado(EstadoComercio.SUSPENDIDO).build()));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UsuarioInactivoException.class);

        verify(jwtService, never()).generarToken(any());
    }

    @Test
    void login_conComercioVinculadoActivo_esExitoso() {
        Usuario usuario = usuarioActivo();
        usuario.setRol(Rol.COMERCIO);
        usuario.setRefId("comercio-1");
        LoginRequest request = new LoginRequest(usuario.getEmail(), "password-correcta");

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-correcta", usuario.getPasswordHash())).thenReturn(true);
        when(comercioRepository.findById("comercio-1"))
                .thenReturn(Optional.of(Comercio.builder().id("comercio-1").estado(EstadoComercio.ACTIVO).build()));
        when(jwtService.generarToken(usuario)).thenReturn("access-token");
        when(jwtService.expiracionEnSegundos()).thenReturn(3600L);
        when(refreshTokenService.generar(usuario.getId())).thenReturn("refresh-token");

        LoginResponse response = authService.login(request);

        assertThat(response.rol()).isEqualTo(Rol.COMERCIO);
    }

    @Test
    void login_conRolAdmin_noConsultaSocioNiComercio() {
        Usuario usuario = usuarioActivo();
        usuario.setRol(Rol.ADMIN);
        usuario.setRefId(null);
        LoginRequest request = new LoginRequest(usuario.getEmail(), "password-correcta");

        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-correcta", usuario.getPasswordHash())).thenReturn(true);
        when(jwtService.generarToken(usuario)).thenReturn("access-token");
        when(jwtService.expiracionEnSegundos()).thenReturn(3600L);
        when(refreshTokenService.generar(usuario.getId())).thenReturn("refresh-token");

        LoginResponse response = authService.login(request);

        assertThat(response.rol()).isEqualTo(Rol.ADMIN);
        verify(socioRepository, never()).findById(any());
        verify(comercioRepository, never()).findById(any());
    }

    // ---------- logout ----------

    @Test
    void logout_conRefreshToken_revocaAmbosTokens() {
        when(jwtService.extraerJti("access-token")).thenReturn("jti-1");
        when(jwtService.extraerExpiracion("access-token")).thenReturn(Instant.now().plusSeconds(60));
        when(jwtService.extraerEmail("access-token")).thenReturn("socio@ruralcuruzu.com");

        authService.logout("access-token", "refresh-token");

        verify(tokenRevocadoService).revocar(eq("jti-1"), any());
        verify(refreshTokenService).revocar("refresh-token");
    }

    @Test
    void logout_sinRefreshToken_soloRevocaAccessToken() {
        when(jwtService.extraerJti("access-token")).thenReturn("jti-1");
        when(jwtService.extraerExpiracion("access-token")).thenReturn(Instant.now().plusSeconds(60));
        when(jwtService.extraerEmail("access-token")).thenReturn("socio@ruralcuruzu.com");

        authService.logout("access-token", null);

        verify(tokenRevocadoService).revocar(eq("jti-1"), any());
        verify(refreshTokenService, never()).revocar(anyString());
    }

    // ---------- forgot password ----------

    @Test
    void solicitarRecuperacionPassword_conEmailExistente_generaTokenYEnviaCorreo() {
        Usuario usuario = usuarioActivo();
        when(rateLimiterService.permitirIntento(anyString(), eq(MAX_SOLICITUDES_RECUPERACION_POR_HORA), any()))
                .thenReturn(true);
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordResetTokenService.generar(usuario.getId())).thenReturn("token-plano");

        authService.solicitarRecuperacionPassword(usuario.getEmail());

        verify(emailService).enviarCorreoRecuperacionPassword(usuario.getEmail(), usuario.getNombre(), "token-plano");
    }

    @Test
    void solicitarRecuperacionPassword_conEmailInexistente_noEnviaCorreoNiFalla() {
        when(rateLimiterService.permitirIntento(anyString(), eq(MAX_SOLICITUDES_RECUPERACION_POR_HORA), any()))
                .thenReturn(true);
        when(usuarioRepository.findByEmail("no-existe@ruralcuruzu.com")).thenReturn(Optional.empty());

        authService.solicitarRecuperacionPassword("no-existe@ruralcuruzu.com");

        verify(emailService, never()).enviarCorreoRecuperacionPassword(anyString(), anyString(), anyString());
        verify(passwordResetTokenService, never()).generar(anyString());
    }

    @Test
    void solicitarRecuperacionPassword_conRateLimitExcedido_noGeneraTokenNiConsultaUsuario() {
        when(rateLimiterService.permitirIntento(anyString(), eq(MAX_SOLICITUDES_RECUPERACION_POR_HORA), any()))
                .thenReturn(false);

        authService.solicitarRecuperacionPassword("socio@ruralcuruzu.com");

        verify(usuarioRepository, never()).findByEmail(anyString());
        verify(emailService, never()).enviarCorreoRecuperacionPassword(anyString(), anyString(), anyString());
    }

    // ---------- reset password ----------

    @Test
    void restablecerPassword_exitoso_actualizaPasswordYNotifica() {
        Usuario usuario = usuarioActivo();
        when(passwordResetTokenService.validarYObtenerUsuarioId("token-plano")).thenReturn(usuario.getId());
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("NuevaPassword1!", usuario.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.encode("NuevaPassword1!")).thenReturn("nuevo-hash");

        authService.restablecerPassword("token-plano", "NuevaPassword1!");

        assertThat(usuario.getPasswordHash()).isEqualTo("nuevo-hash");
        assertThat(usuario.isRequiereCambioPassword()).isFalse();
        verify(usuarioRepository).save(usuario);
        verify(passwordResetTokenService).marcarComoUsado("token-plano");
        verify(emailService).enviarCorreoPasswordCambiada(usuario.getEmail(), usuario.getNombre());
    }

    @Test
    void restablecerPassword_conPasswordIgualALaActual_lanzaPasswordIgual() {
        Usuario usuario = usuarioActivo();
        when(passwordResetTokenService.validarYObtenerUsuarioId("token-plano")).thenReturn(usuario.getId());
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("MismaPassword1!", usuario.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.restablecerPassword("token-plano", "MismaPassword1!"))
                .isInstanceOf(PasswordIgualException.class);

        verify(usuarioRepository, never()).save(any());
        verify(passwordResetTokenService, never()).marcarComoUsado(anyString());
    }

    @Test
    void restablecerPassword_siFallaElEnvioDeCorreo_noPropagaLaExcepcion() {
        Usuario usuario = usuarioActivo();
        when(passwordResetTokenService.validarYObtenerUsuarioId("token-plano")).thenReturn(usuario.getId());
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("NuevaPassword1!", usuario.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.encode("NuevaPassword1!")).thenReturn("nuevo-hash");
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP caído"))
                .when(emailService).enviarCorreoPasswordCambiada(anyString(), anyString());

        // No debe lanzar: la contraseña ya se guardó con éxito, el correo es best-effort.
        authService.restablecerPassword("token-plano", "NuevaPassword1!");

        verify(usuarioRepository).save(usuario);
    }

    // ---------- cambiar password (autenticado) ----------

    @Test
    void cambiarPassword_exitoso_actualizaPasswordYNotifica() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-temporal", usuario.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.matches("NuevaPassword1!", usuario.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.encode("NuevaPassword1!")).thenReturn("nuevo-hash");

        authService.cambiarPassword(usuario.getId(), "password-temporal", "NuevaPassword1!");

        assertThat(usuario.getPasswordHash()).isEqualTo("nuevo-hash");
        assertThat(usuario.isRequiereCambioPassword()).isFalse();
        verify(usuarioRepository).save(usuario);
        verify(emailService).enviarCorreoPasswordCambiada(usuario.getEmail(), usuario.getNombre());
    }

    @Test
    void cambiarPassword_conPasswordActualIncorrecta_lanzaPasswordActualIncorrecta() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-equivocada", usuario.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.cambiarPassword(usuario.getId(), "password-equivocada", "NuevaPassword1!"))
                .isInstanceOf(PasswordActualIncorrectaException.class);

        verify(usuarioRepository, never()).save(any());
        verify(emailService, never()).enviarCorreoPasswordCambiada(anyString(), anyString());
    }

    @Test
    void cambiarPassword_conNuevaPasswordIgualALaActual_lanzaPasswordIgual() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-temporal", usuario.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.matches("MismaPassword1!", usuario.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.cambiarPassword(usuario.getId(), "password-temporal", "MismaPassword1!"))
                .isInstanceOf(PasswordIgualException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarPassword_conUsuarioInexistente_lanzaCredencialesInvalidas() {
        when(usuarioRepository.findById("id-borrado")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.cambiarPassword("id-borrado", "cualquiera", "NuevaPassword1!"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    // ---------- refresh token ----------

    @Test
    void refrescarToken_exitoso_devuelveNuevoAccessTokenYRotaRefreshToken() {
        Usuario usuario = usuarioActivo();
        RefreshTokenService.ResultadoRotacion resultado =
                new RefreshTokenService.ResultadoRotacion(usuario.getId(), "nuevo-refresh-token");

        when(refreshTokenService.rotar("refresh-viejo")).thenReturn(resultado);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socioActivo()));
        when(jwtService.generarToken(usuario)).thenReturn("nuevo-access-token");
        when(jwtService.expiracionEnSegundos()).thenReturn(3600L);

        LoginResponse response = authService.refrescarToken("refresh-viejo");

        assertThat(response.token()).isEqualTo("nuevo-access-token");
        assertThat(response.refreshToken()).isEqualTo("nuevo-refresh-token");
    }

    @Test
    void refrescarToken_conUsuarioInactivo_lanzaUsuarioInactivo() {
        Usuario usuario = usuarioActivo();
        usuario.setEstado(EstadoUsuario.DADO_DE_BAJA);
        RefreshTokenService.ResultadoRotacion resultado =
                new RefreshTokenService.ResultadoRotacion(usuario.getId(), "nuevo-refresh-token");

        when(refreshTokenService.rotar("refresh-viejo")).thenReturn(resultado);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.refrescarToken("refresh-viejo"))
                .isInstanceOf(UsuarioInactivoException.class);

        verify(jwtService, never()).generarToken(any());
    }
}
