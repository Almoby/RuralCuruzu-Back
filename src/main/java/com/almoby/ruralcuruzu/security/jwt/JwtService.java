package com.almoby.ruralcuruzu.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.almoby.ruralcuruzu.enums.Rol;
import com.almoby.ruralcuruzu.domain.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * Genera y valida los JWT que usa la plataforma para autenticar requests
 * luego del login. El token guarda el email (subject), el rol y un id único
 * (jti) que permite revocarlo individualmente en el logout.
 */
@Slf4j
@Component
public class JwtService {

    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_REF_ID = "refId";

    private final SecretKey claveFirma;
    private final long expiracionMinutos;

    public JwtService(@Value("${jwt.secret}") String secreto,
                       @Value("${jwt.expiration-minutes}") long expiracionMinutos) {
        this.claveFirma = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plus(Duration.ofMinutes(expiracionMinutos));
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .id(jti)
                .subject(usuario.getEmail())
                .claims(Map.of(
                        CLAIM_ROL, usuario.getRol().name(),
                        CLAIM_REF_ID, usuario.getRefId() == null ? "" : usuario.getRefId()
                ))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expiracion))
                .signWith(claveFirma)
                .compact();

        log.debug("Token generado jti={} para email={} expira={}", jti, usuario.getEmail(), expiracion);
        return token;
    }

    public long expiracionEnSegundos() {
        return expiracionMinutos * 60;
    }

    public String extraerEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Rol extraerRol(String token) {
        String rol = parseClaims(token).get(CLAIM_ROL, String.class);
        return Rol.valueOf(rol);
    }

    public String extraerJti(String token) {
        return parseClaims(token).getId();
    }

    public Instant extraerExpiracion(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    /**
     * Solo valida firma y expiración (criptografía pura). No sabe nada de revocación:
     * eso lo resuelve TokenRevocadoService, que sí tiene acceso a la base de datos.
     */
    public boolean esValido(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("Token expirado: {}", ex.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Token inválido o manipulado: {}", ex.getMessage());
            return false;
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
