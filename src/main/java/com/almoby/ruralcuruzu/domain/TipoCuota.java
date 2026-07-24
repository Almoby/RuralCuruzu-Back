package com.almoby.ruralcuruzu.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoTipoCuota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tipo de cuota (documento, sección 10.1): define cuánto y cuándo se le cobra
 * a cada categoría de socio (ej. "Cuota de socio activo", "Cuota anual").
 * La generación mensual (10.2) busca, para cada socio activo, el TipoCuota
 * ACTIVO cuya categoría coincida y cuya fechaVigencia sea la más reciente que
 * ya haya llegado: así, si se carga un nuevo importe con una fechaVigencia
 * futura, el importe viejo se sigue usando hasta que esa fecha llegue.
 */
@Document(collection = "tipos_cuota")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoCuota {

    @Id
    private String id;

    @Field("nombre")
    private String nombre;

    @Field("descripcion")
    private String descripcion;

    @Field("categoria_aplicable")
    private CategoriaSocio categoriaAplicable;

    @Field("importe")
    private BigDecimal importe;

    /** Fecha desde la cual este importe/tipo entra en vigencia. */
    @Field("fecha_vigencia")
    private LocalDate fechaVigencia;

    /** Día del mes (1-31) en el que vence la cuota generada con este tipo. */
    @Field("dia_vencimiento")
    private Integer diaVencimiento;

    @Field("estado")
    private EstadoTipoCuota estado;

    @Field("fecha_creacion")
    private Instant fechaCreacion;

    @Field("fecha_actualizacion")
    private Instant fechaActualizacion;

    public boolean estaActivo() {
        return estado == EstadoTipoCuota.ACTIVO;
    }
}
