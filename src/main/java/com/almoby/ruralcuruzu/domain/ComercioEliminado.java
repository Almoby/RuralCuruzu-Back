package com.almoby.ruralcuruzu.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.EstadoComercio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de auditoría ("tombstone") de un comercio que fue eliminado
 * físicamente. A pedido explícito del dueño del proyecto: el borrado de un
 * comercio es un borrado real (el documento Comercio deja de existir en la
 * colección "comercios"), pero tiene que quedar constancia de que existió y
 * de que se dio de baja — por eso, antes de borrar, se guarda acá una copia
 * completa de sus datos más los metadatos de la baja (quién, cuándo, por qué).
 *
 * Es independiente de {@link Comercio}: no tiene una referencia "viva" (no
 * hay find-by-id posible después de borrar), solo una copia de texto, igual
 * que el resto de los patrones de denormalización de este proyecto (ej.
 * HistorialBeneficio).
 */
@Document(collection = "comercios_eliminados")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComercioEliminado {

    @Id
    private String id;

    @Field("comercio_id_original")
    private String comercioIdOriginal;

    @Field("nombre_comercial")
    private String nombreComercial;

    @Field("razon_social")
    private String razonSocial;

    @Field("cuit")
    private String cuit;

    @Field("rubro")
    private String rubro;

    @Field("telefono")
    private String telefono;

    @Field("correo_electronico")
    private String correoElectronico;

    @Field("direccion")
    private String direccion;

    /** Estado que tenía el comercio justo antes de eliminarlo (ACTIVO, INACTIVO, etc.). */
    @Field("estado_al_eliminar")
    private EstadoComercio estadoAlEliminar;

    @Field("fecha_alta")
    private Instant fechaAlta;

    @Field("motivo")
    private String motivo;

    @Field("admin_responsable_baja_id")
    private String adminResponsableBajaId;

    @Field("admin_responsable_baja_nombre")
    private String adminResponsableBajaNombre;

    @Field("fecha_baja")
    private Instant fechaBaja;
}
