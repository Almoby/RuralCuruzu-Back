package com.almoby.ruralcuruzu.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.TipoTokenUnSoloUso;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Token de un solo uso genérico: mismo mecanismo para cualquier flujo que
 * necesite mandar un link de un solo uso por correo (hoy: "olvidé mi
 * contraseña" y "responder una observación de solicitud sin cuenta").
 * Reemplaza lo que antes eran dos clases casi idénticas (PasswordResetToken
 * y TokenRespuestaSolicitud); lo único que cambia entre flujos es
 * {@link #tipo} y a qué apunta {@link #ownerId} (usuarioId, numeroSolicitud,
 * etc.), no viven en colecciones separadas.
 *
 * El {@code _id} es el HASH (SHA-256) del token que se manda por email,
 * nunca el token en texto plano: así, si alguien accede a la base de datos,
 * no puede usar los tokens directamente. El índice TTL borra el documento
 * automáticamente al expirar, aunque nunca haya sido usado.
 */
@Document(collection = "tokens_un_solo_uso")
@CompoundIndex(name = "tipo_owner_idx", def = "{'tipo': 1, 'owner_id': 1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenUnSoloUso {

    @Id
    private String tokenHash;

    @Field("tipo")
    private TipoTokenUnSoloUso tipo;

    /** usuarioId para RESET_PASSWORD, numeroSolicitud para RESPUESTA_SOLICITUD. */
    @Field("owner_id")
    private String ownerId;

    @Indexed(name = "ttl_expiracion", expireAfterSeconds = 0)
    @Field("expira_en")
    private Instant expiraEn;

    @Field("usado")
    private boolean usado;

    @Field("fecha_creacion")
    private Instant fechaCreacion;
}
