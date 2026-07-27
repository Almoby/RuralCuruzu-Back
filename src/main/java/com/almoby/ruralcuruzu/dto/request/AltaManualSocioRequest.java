package com.almoby.ruralcuruzu.dto.request;

import java.time.LocalDate;

import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.validation.CuitCuil;
import com.almoby.ruralcuruzu.validation.DatosPersonaRequestValidable;
import com.almoby.ruralcuruzu.validation.DatosSolicitudValidos;
import com.almoby.ruralcuruzu.validation.Dni;
import com.almoby.ruralcuruzu.validation.Telefono;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

/**
 * Cuerpo del alta manual de un socio por parte del admin (documento, sección
 * 9.5): los mismos datos personales que el formulario público de solicitud
 * (SolicitudSocioRequest, sección 5), pero sin pasar por una SolicitudSocio —
 * el Socio se crea directo, con el estado que el admin elija (por defecto
 * ACTIVO si no se envía). Comparte la misma validación cruzada
 * física/jurídica que el formulario público, vía {@link DatosSolicitudValidos}.
 *
 * {@code apellidoYNombre} sirve tanto para el nombre de una persona física
 * como para la razón social de una persona jurídica, igual que en
 * SolicitudSocioRequest (un solo campo de texto libre, no nombre/apellido
 * separados). {@code nombreEstablecimiento} cubre tanto el nombre de un
 * establecimiento propio como una ocupación, también igual que allá.
 */
@DatosSolicitudValidos
public record AltaManualSocioRequest(

        @Schema(description = "Categoría de asociación")
        @NotNull(message = "La categoría es obligatoria")
        CategoriaSocio categoria,

        @Schema(description = "Si el socio es persona física o jurídica")
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

        @Schema(description = "Dirección del establecimiento, si es distinta de la dirección personal",
                example = "Ruta 123 km 4")
        @NotBlank(message = "La dirección del establecimiento es obligatoria")
        String direccionEstablecimiento,

        @Schema(description = "Solo si tipoPersona es JURIDICA: nombre de la persona responsable",
                example = "María Fernández")
        String nombreResponsable,

        @Schema(description = "Solo si tipoPersona es JURIDICA: DNI de la persona responsable", example = "30.123.456")
        @Dni
        String dniResponsable,

        @Schema(description = "Estado inicial de la membresía. Si no se envía, se da de alta como ACTIVO")
        EstadoSocio estado

) implements DatosPersonaRequestValidable {
}
