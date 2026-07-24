package com.almoby.ruralcuruzu.domain;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos propios de una solicitud hecha por una persona física
 * (documento 5.2). Se guarda embebido dentro de {@link SolicitudSocio};
 * solo existe cuando {@code tipoPersona == FISICA}.
 *
 * {@code apellidoYNombre} es un solo campo de texto libre (ej. "García, Juan
 * Carlos"), no nombre y apellido por separado; y {@code nombreEstablecimiento}
 * cubre tanto el nombre de un establecimiento propio como una ocupación (ej.
 * "Comerciante") en un solo campo. Así lo pide el Figma del formulario
 * "Quiero ser socio".
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatosPersonaFisica {

    @Field("apellido_y_nombre")
    private String apellidoYNombre;

    @Field("dni")
    private String dni;

    @Field("fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Field("cuit_cuil")
    private String cuitCuil;

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

    @Field("direccion_establecimiento")
    private String direccionEstablecimiento;
}
