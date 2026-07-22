package com.almoby.ruralcuruzu.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuenta de acceso a la plataforma. Es independiente del perfil de negocio:
 * un Usuario con rol SOCIO o COMERCIO referencia, a través de {@link #refId},
 * al documento Socio o Comercio correspondiente. Un usuario ADMIN no necesita refId.
 */
@Document(collection = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("email")
    private String email;

    @Field("password_hash")
    private String passwordHash;

    @Field("rol")
    private Rol rol;

    /**
     * Id del documento Socio o Comercio asociado a esta cuenta.
     * Null cuando el rol es ADMIN.
     */
    @Field("ref_id")
    private String refId;

    /**
     * Nombre para mostrar (denormalizado) para no tener que resolver
     * el perfil completo solo para saludar al usuario tras el login.
     */
    @Field("nombre")
    private String nombre;

    @Field("estado")
    private EstadoUsuario estado;

    /**
     * true cuando la contraseña fue generada por el sistema (alta manual, RN-16)
     * y el usuario todavía no la cambió. El frontend debe forzar el cambio.
     */
    @Field("requiere_cambio_password")
    private boolean requiereCambioPassword;

    @Field("fecha_creacion")
    private Instant fechaCreacion;

    public boolean estaActivo() {
        return estado == EstadoUsuario.ACTIVO;
    }
}
