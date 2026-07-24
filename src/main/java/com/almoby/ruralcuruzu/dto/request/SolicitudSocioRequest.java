package com.almoby.ruralcuruzu.dto.request;

import java.time.LocalDate;

import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.validation.CuitCuil;
import com.almoby.ruralcuruzu.validation.DatosSolicitudValidos;
import com.almoby.ruralcuruzu.validation.Dni;
import com.almoby.ruralcuruzu.validation.Telefono;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

/**
 * Cuerpo del formulario de "Solicitud para ser socio" (documento, sección 5),
 * aplanado según el Figma: un solo formulario con todos los campos, sin
 * anidar datosPersonaFisica/datosPersonaJuridica por separado.
 *
 * {@code apellidoYNombre} sirve tanto para el nombre de una persona física
 * como para la razón social de una persona jurídica (un solo campo en el
 * formulario, "Apellido y Nombre / Razón Social"). {@code cuit} igual: es el
 * CUIT/CUIL para ambos tipos.
 *
 * {@code documento} y {@code fechaNacimiento} solo corresponden si
 * {@code tipoPersona == FISICA}; {@code nombreResponsable} y
 * {@code dniResponsable} solo si {@code tipoPersona == JURIDICA}. Ver
 * {@link DatosSolicitudValidos} para la validación cruzada según el tipo.
 */
@DatosSolicitudValidos
public record SolicitudSocioRequest(

        @Schema(description = "Categoría de asociación solicitada")
        @NotNull(message = "La categoría solicitada es obligatoria")
        CategoriaSocio categoriaSolicitada,

        @Schema(description = "Si el solicitante es persona física o jurídica")
        @NotNull(message = "El tipo de persona es obligatorio")
        TipoPersona tipoPersona,

        @Schema(description = "Apellido y nombre (persona física) o razón social (persona jurídica)",
                example = "García, Juan Carlos")
        @NotBlank(message = "El apellido y nombre / razón social es obligatorio")
        String apellidoYNombre,

        @Schema(description = "Solo si tipoPersona es FISICA", example = "28.345.678")
        @Dni
        String documento,

        @Schema(description = "CUIT/CUIL, para ambos tipos de persona", example = "20-28345678-9")
        @NotBlank(message = "El CUIT/CUIL es obligatorio")
        @CuitCuil
        String cuit,

        @Schema(description = "Solo si tipoPersona es FISICA", example = "1985-04-12")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,

        @Schema(example = "Calle 123, Curuzú Cuatiá")
        @NotBlank(message = "La dirección es obligatoria")
        String direccion,

        @Schema(description = "Portal, piso, departamento o referencia adicional", example = "Depto B")
        String portalPisoDepartamento,

        @Schema(example = "+54 9 3777 123456")
        @NotBlank(message = "El teléfono es obligatorio")
        @Telefono
        String telefono,

        @Schema(example = "juan.garcia@example.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @Schema(description = "Nombre del establecimiento o de la ocupación", example = "Farmacia Central")
        @NotBlank(message = "El nombre del establecimiento u ocupación es obligatorio")
        String nombreEstablecimiento,

        @Schema(description = "Dirección del establecimiento, si es distinta de la dirección personal", example = "Ruta 123 km 4")
        @NotBlank(message = "La dirección del establecimiento es obligatoria")
        String direccionEstablecimiento,

        @Schema(description = "Solo si tipoPersona es JURIDICA: nombre de la persona responsable", example = "María Fernández")
        String nombreResponsable,

        @Schema(description = "Solo si tipoPersona es JURIDICA: DNI de la persona responsable", example = "30.123.456")
        @Dni
        String dniResponsable,

        @Schema(description = "El solicitante debe aceptar los términos y condiciones para poder enviar la solicitud")
        @AssertTrue(message = "Debés aceptar los términos y condiciones para continuar")
        boolean aceptaTerminosYCondiciones

) {
}
