package com.almoby.ruralcuruzu.domain;

import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos propios de una solicitud hecha por una persona jurídica
 * (documento 5.2). Se guarda embebido dentro de {@link SolicitudSocio};
 * solo existe cuando {@code tipoPersona == JURIDICA}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatosPersonaJuridica {

    @Field("razon_social")
    private String razonSocial;

    @Field("cuit")
    private String cuit;

    @Field("direccion")
    private String direccion;

    @Field("portal_piso_departamento")
    private String portalPisoDepartamento;

    @Field("telefono")
    private String telefono;

    @Field("correo_electronico")
    private String correoElectronico;

    @Field("nombre_establecimiento")
    private String nombreEstablecimiento;

    @Field("nombre_responsable")
    private String nombreResponsable;

    @Field("dni_responsable")
    private String dniResponsable;

    @Field("direccion_establecimiento")
    private String direccionEstablecimiento;
}
