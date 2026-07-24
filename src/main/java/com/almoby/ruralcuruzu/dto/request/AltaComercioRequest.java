package com.almoby.ruralcuruzu.dto.request;

import com.almoby.ruralcuruzu.enums.EstadoComercio;
import com.almoby.ruralcuruzu.validation.CuitCuil;
import com.almoby.ruralcuruzu.validation.Telefono;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo del alta de un comercio por parte del admin (documento, sección 12.2).
 * No hay un flujo de "solicitud" público como con Socio: el admin carga el
 * comercio directamente. Al crearse, siempre se crea también su Usuario con
 * contraseña temporal y rol COMERCIO (sección 12.3).
 */
public record AltaComercioRequest(

        @Schema(example = "Almacén Don José")
        @NotBlank(message = "El nombre comercial es obligatorio")
        String nombreComercial,

        @Schema(example = "Don José S.R.L.")
        @NotBlank(message = "La razón social es obligatoria")
        String razonSocial,

        @Schema(example = "30-71234567-9")
        @NotBlank(message = "El CUIT es obligatorio")
        @CuitCuil
        String cuit,

        @Schema(example = "Almacén y despensa")
        @NotBlank(message = "El rubro es obligatorio")
        String rubro,

        @Schema(example = "+54 9 3777 123456")
        @NotBlank(message = "El teléfono es obligatorio")
        @Telefono
        String telefono,

        @Schema(example = "contacto@donjose.com")
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no tiene un formato válido")
        String correoElectronico,

        @Schema(example = "Ruta 123 km 4, Curuzú Cuatiá")
        @NotBlank(message = "La dirección es obligatoria")
        String direccion,

        @Schema(description = "URL del logo, opcional")
        String logo,

        @Schema(description = "Descripción del comercio, opcional")
        String descripcion,

        @Schema(description = "Estado inicial. Si no se manda, queda ACTIVO por defecto")
        EstadoComercio estado

) {
}
