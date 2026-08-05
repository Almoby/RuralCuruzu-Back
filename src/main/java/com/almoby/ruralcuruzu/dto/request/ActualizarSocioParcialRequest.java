package com.almoby.ruralcuruzu.dto.request;

import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.validation.Telefono;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

/**
 * Edición de un socio (PATCH /api/admin/socios/{id}): TODOS los campos son
 * opcionales. El service solo actualiza los que vengan con un valor no
 * vacío; cualquier campo omitido, en {@code null} o en blanco queda
 * exactamente como estaba. Permite editar un solo dato (ej. solo el
 * teléfono) sin tener que reenviar el socio entero.
 *
 * <p>A propósito NO incluye los datos identificatorios (DNI, apellido y
 * nombre / razón social, CUIT/CUIL, fecha de nacimiento, nombre/DNI del
 * responsable): esos quedan fijos a lo que se declaró al darse de alta. Solo
 * se pueden editar la categoría y los datos de contacto, comunes a persona
 * física y jurídica.
 */
public record ActualizarSocioParcialRequest(

        @Schema(description = "Categoría de asociación")
        CategoriaSocio categoria,

        @Schema(example = "+54 9 3777 123456")
        @Telefono
        String telefono,

        @Schema(example = "juan.garcia@example.com")
        @Email(message = "El correo electrónico no tiene un formato válido")
        String correoElectronico,

        @Schema(example = "Calle 123, Curuzú Cuatiá")
        String direccion,

        @Schema(description = "Portal, piso, departamento o referencia adicional", example = "Depto B")
        String portalPisoDepartamento,

        @Schema(description = "Nombre del establecimiento o de la ocupación", example = "Farmacia Central")
        String nombreEstablecimiento,

        @Schema(description = "Dirección del establecimiento, si es distinta de la dirección personal")
        String direccionEstablecimiento

) {
}
