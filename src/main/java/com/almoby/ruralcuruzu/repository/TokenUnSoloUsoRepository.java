package com.almoby.ruralcuruzu.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.TokenUnSoloUso;
import com.almoby.ruralcuruzu.enums.TipoTokenUnSoloUso;

public interface TokenUnSoloUsoRepository extends MongoRepository<TokenUnSoloUso, String> {

    /**
     * Borra los tokens de un tipo y dueño sin usar. Se llama al generar uno
     * nuevo, así no quedan varios tokens válidos en simultáneo para el mismo
     * flujo y dueño (ej. dos links de recuperación de contraseña vigentes a
     * la vez para el mismo usuario).
     */
    long deleteByTipoAndOwnerIdAndUsadoFalse(TipoTokenUnSoloUso tipo, String ownerId);
}
