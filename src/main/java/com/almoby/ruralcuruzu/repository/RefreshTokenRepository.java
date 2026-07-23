package com.almoby.ruralcuruzu.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.RefreshToken;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    /**
     * Borra todos los refresh tokens de un usuario. Se usa cuando se detecta
     * el reuso de un token ya revocado (posible robo): por seguridad, se
     * fuerza a que todas las sesiones de ese usuario vuelvan a loguearse.
     */
    long deleteByUsuarioId(String usuarioId);
}
