package com.almoby.ruralcuruzu.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.PasswordResetToken;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    /**
     * Borra los tokens sin usar de un usuario. Se llama al generar uno nuevo,
     * así no quedan varios tokens válidos en simultáneo para la misma cuenta.
     */
    long deleteByUsuarioIdAndUsadoFalse(String usuarioId);
}
