package com.almoby.ruralcuruzu.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoCuota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuota mensual de un socio (documento, sección 10). Los datos del socio y
 * del tipo de cuota se denormalizan (numeroSocio, nombre, tipoCuotaNombre)
 * para no tener que resolverlos en cada listado, igual que en otros módulos
 * de este proyecto (ej. Socio copia los datos de la SolicitudSocio).
 *
 * El índice compuesto único (socioId + periodo) evita, a nivel de base, que
 * la generación mensual cree dos cuotas para el mismo socio en el mismo mes
 * aunque el job se dispare dos veces (defensa en profundidad, además del
 * chequeo explícito que ya hace CuotaServiceImpl antes de generar).
 */
@Document(collection = "cuotas")
@CompoundIndex(name = "socio_periodo_unico", def = "{'socio_id': 1, 'periodo': 1}", unique = true)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cuota {

    @Id
    private String id;

    @Indexed
    @Field("socio_id")
    private String socioId;

    @Field("socio_numero_socio")
    private String socioNumeroSocio;

    @Field("socio_nombre")
    private String socioNombre;

    @Field("tipo_cuota_id")
    private String tipoCuotaId;

    @Field("tipo_cuota_nombre")
    private String tipoCuotaNombre;

    @Field("categoria")
    private CategoriaSocio categoria;

    /** Período que cubre esta cuota, formato "yyyy-MM". */
    @Field("periodo")
    private String periodo;

    @Field("importe")
    private BigDecimal importe;

    @Field("fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Field("estado")
    private EstadoCuota estado;

    @Field("datos_pago")
    private DatosPago datosPago;

    @Field("motivo_rechazo")
    private String motivoRechazo;

    @Field("motivo_anulacion")
    private String motivoAnulacion;

    @Field("fecha_generacion")
    private Instant fechaGeneracion;

    @Field("fecha_actualizacion")
    private Instant fechaActualizacion;

    public boolean estaVencida() {
        return estado == EstadoCuota.PENDIENTE && fechaVencimiento.isBefore(LocalDate.now());
    }
}
