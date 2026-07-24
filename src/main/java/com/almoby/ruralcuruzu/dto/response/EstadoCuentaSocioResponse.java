package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record EstadoCuentaSocioResponse(

        String socioId,
        String socioNumeroSocio,
        String socioNombre,
        BigDecimal deudaTotal,
        List<CuotaResumenResponse> cuotas

) {
}
