package com.almoby.ruralcuruzu.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Token de un solo uso para el flujo de "contraseña olvidada".
 * El {@code _id} es el HASH (SHA-256) del token que se manda por email,
 * nunca el token en texto plano: así, si alguien accede a la base de datos,
 * no puede usar los tokens directamente. El índice TTL borra el documento
 * automáticamente al expirar, aunque nunca haya sido usado.
 */
@Document(collection = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    private String tokenHash;

    @Field("usuario_id")
    private String usuarioId;

    @Indexed(name = "ttl_expiracion", expireAfterSeconds = 0)
    @Field("expira_en")
    private Instant expiraEn;

    @Field("usado")
    private boolean usado;

    @Field("fecha_creacion")
    private Instant fechaCreacion;
}
