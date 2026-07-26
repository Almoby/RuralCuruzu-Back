package com.almoby.ruralcuruzu.dto.request;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * El comercio escanea el QR del socio y elige cuál de sus propios beneficios
 * aplicar (documento, sección 14: "el socio presenta el QR, el comercio lo
 * escanea, si es válido se aplica el descuento"). El ahorro lo indica el
 * comercio en el momento porque depende de la compra real, algo que el
 * sistema no puede calcular solo.
 */
public record ValidarBeneficioRequest(

        @Schema(description = "Código QR mostrado por el socio")
        @NotBlank(message = "El código QR es obligatorio")
        String codigoQr,

        @Schema(description = "Id del beneficio que se está aplicando (debe ser del comercio autenticado)")
        @NotBlank(message = "El beneficio es obligatorio")
        String beneficioId,

        @Schema(description = "Ahorro real de esta aplicación del beneficio", example = "450.00")
        @NotNull(message = "El monto de ahorro es obligatorio")
        @PositiveOrZero(message = "El monto de ahorro no puede ser negativo")
        BigDecimal montoAhorro

) {
}
