package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.almoby.ruralcuruzu.domain.TipoCuota;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoTipoCuota;

public record TipoCuotaResponse(

        String id,
        String nombre,
        String descripcion,
        CategoriaSocio categoriaAplicable,
        BigDecimal importe,
        LocalDate fechaVigencia,
        Integer diaVencimiento,
        EstadoTipoCuota estado,
        Instant fechaCreacion,
        Instant fechaActualizacion

) {

    public static TipoCuotaResponse from(TipoCuota tipoCuota) {
        return new TipoCuotaResponse(
                tipoCuota.getId(),
                tipoCuota.getNombre(),
                tipoCuota.getDescripcion(),
                tipoCuota.getCategoriaAplicable(),
                tipoCuota.getImporte(),
                tipoCuota.getFechaVigencia(),
                tipoCuota.getDiaVencimiento(),
                tipoCuota.getEstado(),
                tipoCuota.getFechaCreacion(),
                tipoCuota.getFechaActualizacion());
    }
}
