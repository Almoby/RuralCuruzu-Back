package com.almoby.ruralcuruzu.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.dto.request.LoginRequest;
import com.almoby.ruralcuruzu.dto.response.LoginResponse;
import com.almoby.ruralcuruzu.exception.CredencialesInvalidasException;
import com.almoby.ruralcuruzu.exception.PasswordIgualException;
import com.almoby.ruralcuruzu.exception.UsuarioInactivoException;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.security.jwt.JwtService;
import com.almoby.ruralcuruzu.service.AuthService;
import com.almoby.ruralcuruzu.service.EmailService;
import com.almoby.ruralcuruzu.service.PasswordResetTokenService;
import com.almoby.ruralcuruzu.service.TokenRevocadoService;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRevocadoService tokenRevocadoService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                            PasswordEncoder passwordEncoder,
                            JwtService jwtService,
                            TokenRevocadoService tokenRevocadoService,
                            PasswordResetTokenService passwordResetTokenService,
                            EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenRevocadoService = tokenRevocadoService;
        this.passwordResetTokenService = passwordResetTokenService;
        this.emailService = emailService;
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

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            log.warn("Contraseña incorrecta para email={}", email);
            throw new CredencialesInvalidasException();
        }

        if (!usuario.estaActivo()) {
            log.warn("Login bloqueado: usuario email={} tiene estado={}", email, usuario.getEstado());
            throw new UsuarioInactivoException();
        }

        String token = jwtService.generarToken(usuario);
        log.info("Token generado para email={} rol={} refId={}", email, usuario.getRol(), usuario.getRefId());

        return LoginResponse.bearer(
                token,
                usuario.getRol(),
                usuario.getNombre(),
                usuario.getRefId(),
                jwtService.expiracionEnSegundos(),
                usuario.isRequiereCambioPassword());
    }

    @Override
    public void logout(String token) {
        String jti = jwtService.extraerJti(token);
        Instant expiracion = jwtService.extraerExpiracion(token);
        String email = jwtService.extraerEmail(token);

        tokenRevocadoService.revocar(jti, expiracion);
        log.info("Logout exitoso para email={} (jti={})", email, jti);
    }

    @Override
    public void solicitarRecuperacionPassword(String email) {
        String emailNormalizado = normalizarEmail(email);

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
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }
}
