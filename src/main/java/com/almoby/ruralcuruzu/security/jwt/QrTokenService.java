package com.almoby.ruralcuruzu.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.almoby.ruralcuruzu.exception.CodigoQrExpiradoException;
import com.almoby.ruralcuruzu.exception.CodigoQrInvalidoException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * Genera y valida el token de corta duración que representa el QR de "Mi QR"
 * (documento, sección 15): "no deberá utilizarse como una imagen fija
 * permanente [...] deberá renovarse automáticamente según las reglas
 * definidas". El QR que ve el socio no es más un código fijo: codifica este
 * token, que vence a los pocos segundos y hay que volver a generar.
 *
 * A diferencia de {@link JwtService} (sesión del usuario, dura horas y sirve
 * para autenticar), este token es de un único propósito: probarle al
 * comercio que el QR que está escaneando fue generado hace muy poco por la
 * app del socio. No identifica una sesión ni reemplaza el login, por eso
 * solo lleva el id del socio y reutiliza la misma clave de firma que
 * JwtService en vez de manejar un secreto aparte.
 */
@Slf4j
@Component
public class QrTokenService {

    private final SecretKey claveFirma;
    private final long validezSegundos;

    public QrTokenService(@Value("${jwt.secret}") String secreto,
                           @Value("${qr.token-validez-segundos}") long validezSegundos) {
        this.claveFirma = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.validezSegundos = validezSegundos;
    }

    /** Genera un token nuevo para el socio, válido por {@link #validezSegundos()}. */
    public QrTokenGenerado generar(String socioId) {
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plus(Duration.ofSeconds(validezSegundos));

        String token = Jwts.builder()
                .subject(socioId)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expiracion))
                .signWith(claveFirma)
                .compact();

        return new QrTokenGenerado(token, expiracion);
    }

    public long validezSegundos() {
        return validezSegundos;
    }

    /**
     * Devuelve el id del socio dueño del token si es válido. Lanza
     * {@link CodigoQrExpiradoException} si venció (el caso esperado cada
     * pocos segundos: hay que pedirle al socio que actualice el QR) o
     * {@link CodigoQrInvalidoException} si está manipulado o mal formado.
     */
    public String extraerSocioId(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (ExpiredJwtException ex) {
            log.debug("Token de QR expirado: {}", ex.getMessage());
            throw new CodigoQrExpiradoException();
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Token de QR inválido o manipulado: {}", ex.getMessage());
            throw new CodigoQrInvalidoException();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(claveFirma)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
