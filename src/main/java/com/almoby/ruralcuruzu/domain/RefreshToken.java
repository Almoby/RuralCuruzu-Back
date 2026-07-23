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
 * Token de larga duración para renovar el access token (JWT) sin pedirle
 * al usuario que vuelva a loguearse cada hora. Igual que PasswordResetToken,
 * el {@code _id} es el hash SHA-256 del valor real (nunca se guarda en texto plano).
 *
 * Rotación: cada vez que se usa para renovar, se marca {@code revocado=true}
 * y se emite uno nuevo (ver RefreshTokenService). Si alguien intenta reusar
 * uno ya revocado, es señal de que el token fue robado.
 */
@Document(collection = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    private String tokenHash;

    @Field("usuario_id")
    private String usuarioId;

    @Indexed(name = "ttl_expiracion", expireAfterSeconds = 0)
    @Field("expira_en")
    private Instant expiraEn;

    @Field("revocado")
    private boolean revocado;

    @Field("fecha_creacion")
    private Instant fechaCreacion;
}
