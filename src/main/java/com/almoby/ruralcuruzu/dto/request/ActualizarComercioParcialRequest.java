package com.almoby.ruralcuruzu.dto.request;

import com.almoby.ruralcuruzu.validation.CuitCuil;
import com.almoby.ruralcuruzu.validation.Telefono;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

/**
 * Edición de un comercio (PATCH /api/admin/comercios/{id}): TODOS los campos
 * son opcionales. El service solo actualiza los que vengan con un valor no
 * vacío; cualquier campo omitido, en {@code null} o en blanco queda
 * exactamente como estaba. Permite editar un solo dato (ej. solo el
 * teléfono) sin tener que reenviar el comercio entero.
 *
 * <p>No hay forma de "vaciar" ninguno de estos campos con este endpoint
 * (mandar {@code null}/blanco significa "no tocar", nunca "borrar") — para
 * nombreComercial/razonSocial/cuit/rubro/telefono/correoElectronico/direccion
 * no tendría sentido de negocio (son datos obligatorios del comercio), y para
 * logo/descripcion no hay, por ahora, otra forma de vaciarlos.
 */
public record ActualizarComercioParcialRequest(

        @Schema(example = "Almacén Don José")
        String nombreComercial,

        @Schema(example = "Don José S.R.L.")
        String razonSocial,

        @Schema(example = "30-71234567-9")
        @CuitCuil
        String cuit,

        @Schema(example = "Almacén y despensa")
        String rubro,

        @Schema(example = "+54 9 3777 123456")
        @Telefono
        String telefono,

        @Schema(example = "contacto@donjose.com")
        @Email(message = "El correo electrónico no tiene un formato válido")
        String correoElectronico,

        @Schema(example = "Ruta 123 km 4, Curuzú Cuatiá")
        String direccion,

        @Schema(description = "URL del logo")
        String logo,

        @Schema(description = "Descripción del comercio")
        String descripcion

) {
}
