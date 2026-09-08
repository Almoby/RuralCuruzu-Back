package com.almoby.ruralcuruzu.dto.request;

import java.time.LocalDate;

import com.almoby.ruralcuruzu.enums.MedioPago;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Corrección de un pago ya registrado (PATCH /api/admin/cuotas/pagos/{pagoId}):
 * TODOS los campos son opcionales, igual que ActualizarSocioParcialRequest. El
 * caso que motivó esto (pedido del dueño del proyecto): el admin registra un
 * pago a nombre de un socio, pero en realidad era otro socio el que pagó.
 *
 * <p>Si viene {@code socioId} y es distinto del socio actual del pago, el
 * service mueve el pago al socio correcto: la cuota vieja vuelve a quedar
 * pendiente (o vencida, según su fecha de vencimiento) y se busca o genera la
 * cuota del socio correcto para el mismo período, que pasa a PAGADA. El
 * importe se recalcula según la cuota del socio correcto (nunca se acepta un
 * importe arbitrario, mismo criterio que registrarPago).
 *
 * <p>Solo se puede editar un pago con estado APROBADO y que no haya sido
 * informado por el propio socio (los informados tienen su propio flujo de
 * aprobar/rechazar, ver revisarPagoInformado).
 */
public record EditarPagoCuotaRequest(

        @Schema(description = "Id del socio correcto, si el pago se había registrado a nombre de otro",
                example = "6a62acadcaa52614a84e907f")
        String socioId,

        @Schema(description = "Fecha real del pago, si se cargó mal")
        LocalDate fecha,

        @Schema(description = "Medio de pago correcto, si se cargó mal")
        MedioPago medioPago,

        @Schema(description = "Observación corregida (reemplaza a la anterior, no se acumula)")
        String observacion

) {
}
