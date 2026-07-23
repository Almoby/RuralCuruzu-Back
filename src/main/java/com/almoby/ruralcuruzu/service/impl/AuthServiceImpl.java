package com.almoby.ruralcuruzu.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.dto.request.LoginRequest;
import com.almoby.ruralcuruzu.dto.response.LoginResponse;
import com.almoby.ruralcuruzu.exception.CredencialesInvalidasException;
import com.almoby.ruralcuruzu.exception.CuentaBloqueadaTemporalmenteException;
import com.almoby.ruralcuruzu.exception.PasswordIgualException;
import com.almoby.ruralcuruzu.exception.RefreshTokenInvalidoException;
import com.almoby.ruralcuruzu.exception.UsuarioInactivoException;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.security.RateLimiterService;
import com.almoby.ruralcuruzu.security.jwt.JwtService;
import com.almoby.ruralcuruzu.service.AuthService;
import com.almoby.ruralcuruzu.service.EmailService;
import com.almoby.ruralcuruzu.service.PasswordResetTokenService;
import com.almoby.ruralcuruzu.service.RefreshTokenService;
import com.almoby.ruralcuruzu.service.TokenRevocadoService;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final String PREFIJO_CLAVE_FORGOT_PASSWORD = "forgot-password:";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRevocadoService tokenRevocadoService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;
    private final RateLimiterService rateLimiterService;
    private final RefreshTokenService refreshTokenService;
    private final int maxIntentosFallidos;
    private final Duration duracionBloqueo;
    private final int maxSolicitudesRecuperacionPorHora;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                            PasswordEncoder passwordEncoder,
                            JwtService jwtService,
                            TokenRevocadoService tokenRevocadoService,
                            PasswordResetTokenService passwordResetTokenService,
                            EmailService emailService,
                            RateLimiterService rateLimiterService,
                            RefreshTokenService refreshTokenService,
                            @Value("${app.login.max-intentos-fallidos:5}") int maxIntentosFallidos,
                            @Value("${app.login.duracion-bloqueo-minutos:15}") long duracionBloqueoMinutos,
                            @Value("${app.password-reset.max-solicitudes-por-hora:3}") int maxSolicitudesRecuperacionPorHora) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenRevocadoService = tokenRevocadoService;
        this.passwordResetTokenService = passwordResetTokenService;
        this.emailService = emailService;
        this.rateLimiterService = rateLimiterService;
        this.refreshTokenService = refreshTokenService;
        this.maxIntentosFallidos = maxIntentosFallidos;
        this.duracionBloqueo = Duration.ofMinutes(duracionBloqueoMinutos);
        this.maxSolicitudesRecuperacionPorHora = maxSolicitudesRecuperacionPorHora;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String email = normalizarEmail(request.email());
        log.debug("Buscando usuario por email={}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("No existe ningún usuario con email={}", email);
                    return new CredencialesInvalidasException();
                });

        log.debug("Usuario encontrado id={} rol={} estado={}", usuario.getId(), usuario.getRol(), usuario.getEstado());

        if (usuario.estaBloqueadoTemporalmente()) {
            log.warn("Login rechazado: cuenta bloqueada temporalmente para email={} hasta={}",
                    email, usuario.getBloqueadoHasta());
            throw new CuentaBloqueadaTemporalmenteException(usuario.getBloqueadoHasta());
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            registrarIntentoFallido(usuario);
            log.warn("Contraseña incorrecta para email={} (intento {}/{})",
                    email, usuario.getIntentosFallidos(), maxIntentosFallidos);
            throw new CredencialesInvalidasException();
        }

        if (usuario.getIntentosFallidos() > 0 || usuario.getBloqueadoHasta() != null) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuarioRepository.save(usuario);
        }

        if (!usuario.estaActivo()) {
            log.warn("Login bloqueado: usuario email={} tiene estado={}", email, usuario.getEstado());
            throw new UsuarioInactivoException();
        }

        String token = jwtService.generarToken(usuario);
        String refreshToken = refreshTokenService.generar(usuario.getId());
        log.info("Token generado para email={} rol={} refId={}", email, usuario.getRol(), usuario.getRefId());

        return LoginResponse.bearer(
                token,
                refreshToken,
                usuario.getRol(),
                usuario.getNombre(),
                usuario.getRefId(),
                jwtService.expiracionEnSegundos(),
                usuario.isRequiereCambioPassword());
    }

    /**
     * Suma un intento fallido y, si llega al umbral, bloquea la cuenta
     * temporalmente y resetea el contador (para que al desbloquearse
     * empiece de nuevo desde 0, no siga sumando).
     */
    private void registrarIntentoFallido(Usuario usuario) {
        int intentos = usuario.getIntentosFallidos() + 1;

        if (intentos >= maxIntentosFallidos) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(Instant.now().plus(duracionBloqueo));
            log.warn("Cuenta bloqueada temporalmente por intentos fallidos, usuarioId={} hasta={}",
                    usuario.getId(), usuario.getBloqueadoHasta());
        } else {
            usuario.setIntentosFallidos(intentos);
        }

        usuarioRepository.save(usuario);
    }

    @Override
    public void logout(String token, String refreshToken) {
        String jti = jwtService.extraerJti(token);
        Instant expiracion = jwtService.extraerExpiracion(token);
        String email = jwtService.extraerEmail(token);

        tokenRevocadoService.revocar(jti, expiracion);

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revocar(refreshToken);
        }

        log.info("Logout exitoso para email={} (jti={})", email, jti);
    }

    @Override
    public LoginResponse refrescarToken(String refreshToken) {
        RefreshTokenService.ResultadoRotacion resultado = refreshTokenService.rotar(refreshToken);

        Usuario usuario = usuarioRepository.findById(resultado.usuarioId())
                .orElseThrow(() -> {
                    log.warn("Refresh token válido pero usuarioId={} ya no existe", resultado.usuarioId());
                    return new RefreshTokenInvalidoException();
                });

        if (!usuario.estaActivo()) {
            log.warn("Refresh rechazado: usuario email={} tiene estado={}", usuario.getEmail(), usuario.getEstado());
            throw new UsuarioInactivoException();
        }

        String nuevoAccessToken = jwtService.generarToken(usuario);
        log.info("Access token renovado vía refresh para email={}", usuario.getEmail());

        return LoginResponse.bearer(
                nuevoAccessToken,
                resultado.nuevoRefreshToken(),
                usuario.getRol(),
                usuario.getNombre(),
                usuario.getRefId(),
                jwtService.expiracionEnSegundos(),
                usuario.isRequiereCambioPassword());
    }

    @Override
    public void solicitarRecuperacionPassword(String email) {
        String emailNormalizado = normalizarEmail(email);

        boolean permitido = rateLimiterService.permitirIntento(
                PREFIJO_CLAVE_FORGOT_PASSWORD + emailNormalizado, maxSolicitudesRecuperacionPorHora, Duration.ofHours(1));

        if (!permitido) {
            // No se genera token ni se manda correo, pero la respuesta al cliente
            // (ver AuthController) es siempre la misma: no revelamos que existe
            // un límite ni que el email está siendo "atacado".
            log.warn("Demasiadas solicitudes de recuperación de contraseña para email={}, se ignora esta solicitud",
                    emailNormalizado);
            return;
        }

        usuarioRepository.findByEmail(emailNormalizado).ifPresentOrElse(
                usuario -> {
                    String tokenPlano = passwordResetTokenService.generar(usuario.getId());
                    emailService.enviarCorreoRecuperacionPassword(usuario.getEmail(), usuario.getNombre(), tokenPlano);
                    log.info("Solicitud de recuperación de contraseña procesada para email={}", emailNormalizado);
                },
                () -> log.info("Solicitud de recuperación para email={} que no existe (no se revela al cliente)",
                        emailNormalizado));
    }

    @Override
    public void restablecerPassword(String token, String nuevaPassword) {
        String usuarioId = passwordResetTokenService.validarYObtenerUsuarioId(token);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> {
                    // El token era válido pero el usuario dueño ya no existe (dado de baja, por ejemplo).
                    log.warn("Token de recuperación válido pero usuarioId={} ya no existe", usuarioId);
                    return new CredencialesInvalidasException();
                });

        if (passwordEncoder.matches(nuevaPassword, usuario.getPasswordHash())) {
            log.warn("Intento de restablecer contraseña con la misma contraseña actual, usuarioId={}", usuarioId);
            throw new PasswordIgualException();
        }

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setRequiereCambioPassword(false);
        usuarioRepository.save(usuario);

        passwordResetTokenService.marcarComoUsado(token);
        log.info("Contraseña restablecida con éxito para usuarioId={}", usuarioId);

        try {
            emailService.enviarCorreoPasswordCambiada(usuario.getEmail(), usuario.getNombre());
        } catch (RuntimeException ex) {
            // La contraseña ya se cambió con éxito: un fallo al avisar por correo
            // no debe hacer fallar la respuesta del endpoint. Solo se loguea.
            log.warn("No se pudo enviar el correo de confirmación de cambio de contraseña, usuarioId={}",
                    usuarioId, ex);
        }
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }
}
