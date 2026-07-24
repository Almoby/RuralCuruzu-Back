package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RegistrarPagoResponse(

        String mensaje,
        BigDecimal montoTotal,
        List<CuotaResponse> cuotas

) {

    public static RegistrarPagoResponse of(List<CuotaResponse> cuotas) {
        BigDecimal montoTotal = cuotas.stream()
                .map(CuotaResponse::importe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RegistrarPagoResponse("Pago registrado con éxito", montoTotal, cuotas);
    }
}
