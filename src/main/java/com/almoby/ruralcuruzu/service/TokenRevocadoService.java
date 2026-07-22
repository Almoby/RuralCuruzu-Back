package com.almoby.ruralcuruzu.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.TokenRevocado;
import com.almoby.ruralcuruzu.repository.TokenRevocadoRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Maneja la lista negra de tokens (logout). Es un servicio de infraestructura
 * de seguridad, no una regla de negocio del dominio, por eso no tiene interfaz
 * propia como AuthService: solo hay (y solo va a haber) una implementación.
 */
@Slf4j
@Service
public class TokenRevocadoService {

    private final TokenRevocadoRepository repository;

    public TokenRevocadoService(TokenRevocadoRepository repository) {
        this.repository = repository;
    }

    public void revocar(String jti, Instant expiracion) {
        TokenRevocado revocado = new TokenRevocado(jti, expiracion, Instant.now());
        repository.save(revocado);
        log.info("Token revocado jti={} (expira {})", jti, expiracion);
    }

    public boolean estaRevocado(String jti) {
        boolean revocado = repository.existsById(jti);
        if (revocado) {
            log.debug("Token jti={} está en la lista negra, se rechaza", jti);
        }
        return revocado;
    }
}
