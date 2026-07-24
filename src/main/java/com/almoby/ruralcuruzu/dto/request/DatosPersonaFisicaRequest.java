package com.almoby.ruralcuruzu.dto.request;

import java.time.LocalDate;

import com.almoby.ruralcuruzu.validation.CuitCuil;
import com.almoby.ruralcuruzu.validation.Dni;
import com.almoby.ruralcuruzu.validation.Telefono;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

/**
 * Datos exigidos cuando la solicitud es de una persona física (documento 5.2).
 */
public record DatosPersonaFisicaRequest(

        @Schema(example = "Juan Carlos")
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @Schema(example = "García")
        @NotBlank(message = "El apellido es obligatorio")
        String apellido,

        @Schema(example = "28.345.678")
        @NotBlank(message = "El DNI es obligatorio")
        @Dni
        String dni,

        @Schema(example = "1985-04-12")
        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,

        @Schema(example = "20-28345678-9")
        @NotBlank(message = "El CUIT/CUIL es obligatorio")
        @CuitCuil
        String cuitCuil,

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

        @Schema(example = "Comerciante")
        String ocupacion,

        @Schema(description = "Solo si tiene un establecimiento propio", example = "Farmacia Central")
        String nombreEstablecimiento,

        @Schema(description = "Dirección del establecimiento, si es distinta de la dirección personal", example = "Ruta 123 km 4")
        String direccionEstablecimiento

) {
}
