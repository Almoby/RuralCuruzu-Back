package com.almoby.ruralcuruzu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** El admin aprueba o rechaza un pago que un socio informó (estado EN_REVISION). */
public record RevisarPagoInformadoRequest(

        @NotNull(message = "Hay que indicar si se aprueba o se rechaza")
        Boolean aprobar,

        @Schema(description = "Obligatorio si se rechaza", example = "El comprobante no corresponde al importe informado")
        String motivoRechazo

) {
}
