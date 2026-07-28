package com.almoby.ruralcuruzu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Crea o actualiza (upsert) los datos bancarios de la cooperativa. Es un singleton, sin id en la URL. */
public record ActualizarDatosBancariosRequest(

        @Schema(example = "Banco Nación Argentina")
        @NotBlank(message = "El banco es obligatorio")
        String banco,

        @Schema(example = "0110052830052400052001")
        @NotBlank(message = "El CBU es obligatorio")
        String cbu,

        @Schema(example = "COOPERATIVA.UNION")
        @NotBlank(message = "El alias es obligatorio")
        String alias,

        @Schema(example = "Sociedad Rural de Curuzú Cuatiá")
        @NotBlank(message = "El titular es obligatorio")
        String titular,

        @Schema(example = "30-71234567-1")
        @NotBlank(message = "El CUIT es obligatorio")
        String cuit

) {
}
