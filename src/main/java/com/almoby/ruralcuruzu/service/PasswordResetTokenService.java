package com.almoby.ruralcuruzu.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.PasswordResetToken;
import com.almoby.ruralcuruzu.exception.TokenRecuperacionExpiradoException;
import com.almoby.ruralcuruzu.exception.TokenRecuperacionInvalidoException;
import com.almoby.ruralcuruzu.repository.PasswordResetTokenRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Genera y valida los tokens de un solo uso para "olvidé mi contraseña".
 * Servicio de infraestructura de seguridad (igual que TokenRevocadoService):
 * no tiene interfaz propia porque solo va a haber una implementación.
 *
 * El token que se manda por email nunca se guarda en la base tal cual:
 * se guarda su hash SHA-256. Así, aunque alguien acceda a la colección,
 * no puede reconstruir un token válido.
 */
@Slf4j
@Service
public class PasswordResetTokenService {

    private static final int BYTES_DE_ENTROPIA = 32;
    private static final String ALGORITMO_HASH = "SHA-256";

    private final PasswordResetTokenRepository repository;
    private final SecureRandom generadorAleatorio = new SecureRandom();
    private final long expiracionMinutos;

    public PasswordResetTokenService(
            PasswordResetTokenRepository repository,
            @Value("${app.password-reset.expiration-minutes:30}") long expiracionMinutos) {
        this.repository = repository;
        this.expiracionMinutos = expiracionMinutos;
    }

    /**
     * Genera un nuevo token, guarda su hash y devuelve el token EN TEXTO PLANO
     * (el único momento en que existe: no queda persistido en ningún lado).
     */
    public String generar(String usuarioId) {
        long invalidados = repository.deleteByUsuarioIdAndUsadoFalse(usuarioId);
        if (invalidados > 0) {
            log.info("Se invalidaron {} token(s) de recuperación anteriores sin usar, usuarioId={}",
                    invalidados, usuarioId);
        }

        byte[] bytesAleatorios = new byte[BYTES_DE_ENTROPIA];
        generadorAleatorio.nextBytes(bytesAleatorios);
        String tokenPlano = Base64.getUrlEncoder().withoutPadding().encodeToString(bytesAleatorios);

        Instant ahora = Instant.now();
        PasswordResetToken token = new PasswordResetToken(
                hashear(tokenPlano),
                usuarioId,
                ahora.plus(Duration.ofMinutes(expiracionMinutos)),
                false,
                ahora);

        repository.save(token);
        log.info("Token de recuperación generado para usuarioId={} (expira en {} min)", usuarioId, expiracionMinutos);
        return tokenPlano;
    }

    /**
     * Valida el token recibido (sin usar todavía) y devuelve el id del usuario
     * dueño del token si es válido.
     *
     * @throws TokenRecuperacionInvalidoException si no existe o ya fue usado
     * @throws TokenRecuperacionExpiradoException si existe, no fue usado, pero venció
     */
    public String validarYObtenerUsuarioId(String tokenPlano) {
        PasswordResetToken token = buscarPorTokenPlano(tokenPlano)
                .orElseThrow(() -> {
                    log.warn("Token de recuperación no encontrado (hash no coincide con ninguno)");
                    return new TokenRecuperacionInvalidoException();
                });

        if (token.isUsado()) {
            log.warn("Token de recuperación ya usado, usuarioId={}", token.getUsuarioId());
            throw new TokenRecuperacionInvalidoException();
        }

        if (token.getExpiraEn().isBefore(Instant.now())) {
            log.warn("Token de recuperación expirado, usuarioId={} expiraba={}", token.getUsuarioId(), token.getExpiraEn());
            throw new TokenRecuperacionExpiradoException();
        }

        return token.getUsuarioId();
    }

    /**
     * Marca el token como usado para que no pueda volver a canjearse.
     * Se llama recién después de que la nueva contraseña se guardó con éxito.
     */
    public void marcarComoUsado(String tokenPlano) {
        buscarPorTokenPlano(tokenPlano).ifPresent(token -> {
            token.setUsado(true);
            repository.save(token);
            log.info("Token de recuperación marcado como usado, usuarioId={}", token.getUsuarioId());
        });
    }

    private Optional<PasswordResetToken> buscarPorTokenPlano(String tokenPlano) {
        return repository.findById(hashear(tokenPlano));
    }

    private String hashear(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO_HASH);
            byte[] hash = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 es estándar en cualquier JVM: si esto pasa, el entorno está roto.
            throw new IllegalStateException("Algoritmo de hash no disponible: " + ALGORITMO_HASH, ex);
        }
    }
}
