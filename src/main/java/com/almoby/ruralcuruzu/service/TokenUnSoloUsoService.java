package com.almoby.ruralcuruzu.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.TokenUnSoloUso;
import com.almoby.ruralcuruzu.enums.TipoTokenUnSoloUso;
import com.almoby.ruralcuruzu.repository.TokenUnSoloUsoRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Mecánica compartida de generación/validación de tokens de un solo uso
 * (hash SHA-256, TTL, un solo uso). Servicio de infraestructura interno:
 * no lo llama nadie fuera de los servicios "wrapper" específicos de cada
 * flujo ({@link PasswordResetTokenService}, {@link TokenRespuestaSolicitudService}),
 * que son los que exponen los métodos de negocio (con sus propios nombres,
 * excepciones y tiempos de expiración) al resto de la aplicación.
 *
 * Extraído para no repetir esta misma lógica (generar bytes aleatorios,
 * hashear, guardar, buscar por hash, invalidar tokens anteriores) por cada
 * flujo nuevo que necesite un link de un solo uso por correo.
 */
@Slf4j
@Service
public class TokenUnSoloUsoService {

    private static final int BYTES_DE_ENTROPIA = 32;
    private static final String ALGORITMO_HASH = "SHA-256";

    private final TokenUnSoloUsoRepository repository;
    private final SecureRandom generadorAleatorio = new SecureRandom();

    public TokenUnSoloUsoService(TokenUnSoloUsoRepository repository) {
        this.repository = repository;
    }

    /**
     * Genera un nuevo token, guarda su hash y devuelve el token EN TEXTO
     * PLANO (el único momento en que existe: no queda persistido en ningún
     * lado). Invalida cualquier token anterior sin usar del mismo tipo y
     * dueño.
     */
    public String generar(TipoTokenUnSoloUso tipo, String ownerId, long expiracionMinutos) {
        long invalidados = repository.deleteByTipoAndOwnerIdAndUsadoFalse(tipo, ownerId);
        if (invalidados > 0) {
            log.info("Se invalidaron {} token(s) anteriores sin usar, tipo={} ownerId={}", invalidados, tipo, ownerId);
        }

        byte[] bytesAleatorios = new byte[BYTES_DE_ENTROPIA];
        generadorAleatorio.nextBytes(bytesAleatorios);
        String tokenPlano = Base64.getUrlEncoder().withoutPadding().encodeToString(bytesAleatorios);

        Instant ahora = Instant.now();
        TokenUnSoloUso token = new TokenUnSoloUso(
                hashear(tokenPlano),
                tipo,
                ownerId,
                ahora.plus(Duration.ofMinutes(expiracionMinutos)),
                false,
                ahora);

        repository.save(token);
        log.info("Token generado tipo={} ownerId={} (expira en {} min)", tipo, ownerId, expiracionMinutos);
        return tokenPlano;
    }

    /**
     * Busca el token por su valor en texto plano (hasheándolo para comparar
     * contra lo guardado). No valida usado/expirado: eso queda a cargo de
     * cada wrapper, porque cada uno lanza sus propias excepciones específicas.
     */
    public Optional<TokenUnSoloUso> buscarPorTokenPlano(String tokenPlano) {
        return repository.findById(hashear(tokenPlano));
    }

    /** Marca el token como usado para que no pueda volver a canjearse. */
    public void marcarComoUsado(String tokenPlano) {
        buscarPorTokenPlano(tokenPlano).ifPresent(token -> {
            token.setUsado(true);
            repository.save(token);
            log.info("Token marcado como usado, tipo={} ownerId={}", token.getTipo(), token.getOwnerId());
        });
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
