package com.almoby.ruralcuruzu.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.EstadoComercio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Comercio adherido, dado de alta directamente por un admin (documento,
 * sección 12: no hay un formulario público de "solicitud" como con Socio).
 * Al crearse, siempre se crea también su Usuario con contraseña temporal y
 * rol COMERCIO (sección 12.3).
 */
@Document(collection = "comercios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comercio {

    @Id
    private String id;

    @Field("nombre_comercial")
    private String nombreComercial;

    @Field("razon_social")
    private String razonSocial;

    @Indexed(unique = true)
    @Field("cuit")
    private String cuit;

    @Field("rubro")
    private String rubro;

    @Field("telefono")
    private String telefono;

    @Indexed(unique = true)
    @Field("correo_electronico")
    private String correoElectronico;

    @Field("direccion")
    private String direccion;

    /** URL del logo. Opcional. */
    @Field("logo")
    private String logo;

    @Field("descripcion")
    private String descripcion;

    @Field("estado")
    private EstadoComercio estado;

    /** Id del Usuario con el que este comercio inicia sesión. */
    @Indexed(unique = true, sparse = true)
    @Field("usuario_id")
    private String usuarioId;

    @Field("admin_responsable_alta_id")
    private String adminResponsableAltaId;

    @Field("admin_responsable_alta_nombre")
    private String adminResponsableAltaNombre;

    @Field("fecha_alta")
    private Instant fechaAlta;

    @Field("fecha_actualizacion")
    private Instant fechaActualizacion;

    public boolean estaActivo() {
        return estado == EstadoComercio.ACTIVO;
    }
}
