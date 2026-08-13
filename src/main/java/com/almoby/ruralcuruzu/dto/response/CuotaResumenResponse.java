package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.almoby.ruralcuruzu.domain.Cuota;
import com.almoby.ruralcuruzu.domain.Pago;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoCuota;

/**
 * Fila de listado, sin todos los detalles de CuotaResponse.
 * {@code pagoVigente} es, de todos los {@link Pago} de esta cuota, el más
 * relevante para mostrar (el APROBADO si existe, si no el EN_REVISION, si no
 * {@code null}) — ver el mismo criterio en CuotaResponse. {@code categoria}
 * es la del socio al momento de generarse la cuota (denormalizada, igual que
 * socioNombre): permite que el front separe listados como "Cuotas cobradas
 * por mes" entre socios ACTIVO y ADHERENTE sin pedir el socio aparte.
 */
public record CuotaResumenResponse(

        String id,
        String socioNumeroSocio,
        String socioNombre,
        CategoriaSocio categoria,
        String periodo,
        BigDecimal importe,
        EstadoCuota estado,
        LocalDate fechaVencimiento,
        PagoResponse pagoVigente

) {

    public static CuotaResumenResponse from(Cuota cuota, Pago pagoVigente) {
        return new CuotaResumenResponse(
                cuota.getId(),
                cuota.getSocioNumeroSocio(),
                cuota.getSocioNombre(),
                cuota.getCategoria(),
                cuota.getPeriodo(),
                cuota.getImporte(),
                cuota.getEstado(),
                cuota.getFechaVencimiento(),
                PagoResponse.from(pagoVigente));
    }
}
