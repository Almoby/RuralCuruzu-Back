package com.almoby.ruralcuruzu.dto.request;

import com.almoby.ruralcuruzu.validation.CuitCuil;
import com.almoby.ruralcuruzu.validation.Dni;
import com.almoby.ruralcuruzu.validation.Telefono;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Datos exigidos cuando la solicitud es de una persona jurídica (documento 5.2).
 */
public record DatosPersonaJuridicaRequest(

        @Schema(example = "Agropecuaria Curuzú S.A.")
        @NotBlank(message = "La razón social es obligatoria")
        String razonSocial,

        @Schema(example = "30-71234567-8")
        @NotBlank(message = "El CUIT es obligatorio")
        @CuitCuil
        String cuit,

        @Schema(example = "Ruta 123 km 4, Curuzú Cuatiá")
        @NotBlank(message = "La dirección es obligatoria")
        String direccion,

        @Schema(description = "Portal, piso, departamento o referencia adicional")
        String portalPisoDepartamento,

        @Schema(example = "+54 9 3777 123456")
        @NotBlank(message = "El teléfono es obligatorio")
        @Telefono
        String telefono,

        @Schema(example = "contacto@agropecuariacuruzu.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @Schema(example = "Agropecuaria Curuzú")
        @NotBlank(message = "El nombre del establecimiento es obligatorio")
        String nombreEstablecimiento,

        @Schema(example = "María Fernández")
        @NotBlank(message = "El nombre del responsable es obligatorio")
        String nombreResponsable,

        @Schema(example = "30.123.456")
        @NotBlank(message = "El DNI del responsable es obligatorio")
        @Dni
        String dniResponsable

) {
}
