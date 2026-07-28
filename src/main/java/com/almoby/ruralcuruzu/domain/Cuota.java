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
 * Cuota mensual de un socio (documento, sección 10). Los datos del socio se
 * denormalizan (numeroSocio, nombre) para no tener que resolverlos en cada
 * listado, igual que en otros módulos de este proyecto (ej. Socio copia los
 * datos de la SolicitudSocio). tipoCuotaNombre es una etiqueta descriptiva
 * (ej. "Cuota de socio activo"), tomada de {@link com.almoby.ruralcuruzu.domain.ReglaCuota}
 * al generar la cuota: es una copia de texto, no una referencia viva a esa regla
 * (si después se cambia el nombre de la regla, las cuotas ya generadas no se alteran).
 *
 * El índice compuesto único (socioId + periodo) evita, a nivel de base, que
 * la generación mensual cree dos cuotas para el mismo socio en el mismo mes
 * aunque el job se dispare dos veces (defensa en profundidad, además del
 * chequeo explícito que ya hace CuotaServiceImpl antes de generar).
 *
 * Los datos del pago en sí NO viven acá (RN-17: el pago es su propia
 * entidad de base de datos): ver {@link Pago}, que referencia esta cuota por
 * {@code cuotaId}. Una cuota puede tener más de un Pago a lo largo del
 * tiempo (ej. una transferencia rechazada y un segundo intento aprobado).
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
