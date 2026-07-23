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

import com.almoby.ruralcuruzu.domain.RefreshToken;
import com.almoby.ruralcuruzu.exception.RefreshTokenExpiradoException;
import com.almoby.ruralcuruzu.exception.RefreshTokenInvalidoException;
import com.almoby.ruralcuruzu.repository.RefreshTokenRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Genera y rota los refresh tokens (para renovar el access token sin
 * obligar a un re-login cada hora). Servicio de infraestructura de
 * seguridad, igual que TokenRevocadoService y PasswordResetTokenService.
 *
 * Rotación con detección de reuso: cada refresh token sirve una sola vez.
 * Al usarlo, se marca revocado y se emite uno nuevo. Si alguien presenta
 * un token ya revocado (por ejemplo porque un atacante robó uno viejo y
 * el dueño real ya lo usó), se interpreta como señal de robo y se borran
 * TODOS los refresh tokens de ese usuario, forzando a loguearse de nuevo
 * en todas las sesiones.
 */
@Slf4j
@Service
public class RefreshTokenService {

    private static final int BYTES_DE_ENTROPIA = 32;
    private static final String ALGORITMO_HASH = "SHA-256";

    private final RefreshTokenRepository repository;
    private final SecureRandom generadorAleatorio = new SecureRandom();
    private final Duration duracion;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            @Value("${app.refresh-token.expiration-days:7}") long expiracionDias) {
        this.repository = repository;
        this.duracion = Duration.ofDays(expiracionDias);
    }

    /**
     * Genera un nuevo refresh token para el usuario y devuelve el valor en
     * texto plano (el único momento en que existe fuera de su hash).
     * No invalida los refresh tokens existentes: cada login (o cada
     * dispositivo) puede tener el suyo en simultáneo.
     */
    public String generar(String usuarioId) {
        byte[] bytesAleatorios = new byte[BYTES_DE_ENTROPIA];
        generadorAleatorio.nextBytes(bytesAleatorios);
        String tokenPlano = Base64.getUrlEncoder().withoutPadding().encodeToString(bytesAleatorios);

        Instant ahora = Instant.now();
        RefreshToken token = new RefreshToken(
                hashear(tokenPlano), usuarioId, ahora.plus(duracion), false, ahora);

        repository.save(token);
        log.info("Refresh token generado para usuarioId={} (expira en {} días)", usuarioId, duracion.toDays());
        return tokenPlano;
    }

    /**
     * Valida el refresh token, lo rota (revoca el viejo, emite uno nuevo) y
     * devuelve el id del usuario junto con el nuevo token en texto plano.
     *
     * @throws RefreshTokenInvalidoException si no existe, ya fue usado (posible robo,
     *         en cuyo caso además se revocan todos los refresh tokens del usuario)
     * @throws RefreshTokenExpiradoException si existe, no fue usado, pero venció
     */
    public ResultadoRotacion rotar(String tokenPlano) {
        RefreshToken token = buscarPorTokenPlano(tokenPlano)
                .orElseThrow(() -> {
                    log.warn("Refresh token no encontrado (hash no coincide con ninguno)");
                    return new RefreshTokenInvalidoException();
                });

        if (token.isRevocado()) {
            log.warn("Refresh token ya revocado reutilizado (posible robo), usuarioId={}. "
                    + "Revocando todos los refresh tokens de ese usuario.", token.getUsuarioId());
            repository.deleteByUsuarioId(token.getUsuarioId());
            throw new RefreshTokenInvalidoException();
        }

        if (token.getExpiraEn().isBefore(Instant.now())) {
            log.warn("Refresh token expirado, usuarioId={} expiraba={}", token.getUsuarioId(), token.getExpiraEn());
            throw new RefreshTokenExpiradoException();
        }

        token.setRevocado(true);
        repository.save(token);

        String nuevoTokenPlano = generar(token.getUsuarioId());
        log.info("Refresh token rotado con éxito, usuarioId={}", token.getUsuarioId());
        return new ResultadoRotacion(token.getUsuarioId(), nuevoTokenPlano);
    }

    /**
     * Revoca explícitamente un refresh token (logout). A diferencia del
     * reuso detectado en rotar(), esto es una revocación normal: no dispara
     * la limpieza de todos los tokens del usuario.
     */
    public void revocar(String tokenPlano) {
        buscarPorTokenPlano(tokenPlano).ifPresent(token -> {
            token.setRevocado(true);
            repository.save(token);
            log.info("Refresh token revocado por logout, usuarioId={}", token.getUsuarioId());
        });
    }

    private Optional<RefreshToken> buscarPorTokenPlano(String tokenPlano) {
        return repository.findById(hashear(tokenPlano));
    }

    private String hashear(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO_HASH);
            byte[] hash = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo de hash no disponible: " + ALGORITMO_HASH, ex);
        }
    }

    public record ResultadoRotacion(String usuarioId, String nuevoRefreshToken) {
    }
}
