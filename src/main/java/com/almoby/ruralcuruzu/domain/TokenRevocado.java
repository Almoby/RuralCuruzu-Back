package com.almoby.ruralcuruzu.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Lista negra de tokens invalidados por logout. El _id es directamente el
 * jti del token, así la búsqueda es por clave primaria (rápida).
 * expiraEn tiene un índice TTL: Mongo borra el documento solo apenas ese
 * instante pasa, así la colección nunca crece indefinidamente
 * (no tiene sentido seguir bloqueando un token que ya expiró solo).
 */
@Document(collection = "tokens_revocados")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRevocado {

    @Id
    private String jti;

    @Indexed(name = "ttl_expiracion", expireAfterSeconds = 0)
    @Field("expira_en")
    private Instant expiraEn;

    @Field("fecha_revocacion")
    private Instant fechaRevocacion;
}
