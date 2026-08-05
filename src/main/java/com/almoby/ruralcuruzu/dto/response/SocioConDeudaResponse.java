package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;

/**
 * Fila de "Deuda acumulada por socio" / "Socios con cuota vencida" (pantalla
 * de Reportes): un socio con al menos una cuota VENCIDA, con el monto total
 * adeudado (suma de esas cuotas) y cuántas son. Ordenado de mayor a menor
 * deuda, sin límite: el front recorta el top N que necesite mostrar (mismo
 * criterio que BeneficioMasUtilizadoResponse).
 */
public record SocioConDeudaResponse(

        String socioId,
        String numeroSocio,
        String nombre,
        BigDecimal montoAdeudado,
        long cantidadCuotasVencidas

) {
}
