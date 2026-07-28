package com.almoby.ruralcuruzu.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.EstadoPago;
import com.almoby.ruralcuruzu.enums.MedioPago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un intento de pago de una {@link Cuota} (RN-17: el pago es su propia
 * entidad de base de datos, no un dato embebido en la cuota). Una misma
 * cuota puede tener varios Pago a lo largo del tiempo — por ejemplo, una
 * transferencia rechazada y un segundo intento posterior aprobado — y
 * ninguno se sobrescribe ni se borra: quedan como historial completo e
 * inmutable (RN-17: "el historial de pagos [...] no podrá eliminarse").
 *
 * Los datos del socio se denormalizan (numeroSocio, nombre, periodo) para no
 * tener que resolverlos en cada listado, igual que en el resto del proyecto
 * (ej. Cuota ya hace lo mismo con los datos del socio).
 */
@Document(collection = "pagos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    private String id;

    @Indexed
    @Field("cuota_id")
    private String cuotaId;

    @Indexed
    @Field("socio_id")
    private String socioId;

    @Field("socio_numero_socio")
    private String socioNumeroSocio;

    @Field("socio_nombre")
    private String socioNombre;

    @Field("periodo")
    private String periodo;

    @Field("importe")
    private BigDecimal importe;

    @Field("medio_pago")
    private MedioPago medioPago;

    @Field("estado")
    private EstadoPago estado;

    /** Fecha del pago en sí (la que informa el socio, o la real de la transacción). */
    @Field("fecha_pago")
    private Instant fechaPago;

    /** Ruta relativa devuelta por AlmacenamientoComprobantesService, null si no se adjuntó nada. */
    @Field("comprobante_ruta")
    private String comprobanteRuta;

    @Field("observacion")
    private String observacion;

    /** true si lo cargó el propio socio (transferencia informada o link de pago), false si lo cargó un admin. */
    @Field("informado_por_socio")
    private boolean informadoPorSocio;

    @Field("registrado_por_admin_id")
    private String registradoPorAdminId;

    @Field("registrado_por_admin_nombre")
    private String registradoPorAdminNombre;

    /** Obligatorio cuando estado queda en RECHAZADO. */
    @Field("motivo_rechazo")
    private String motivoRechazo;

    /** Id de la preferencia de pago en Mercado Pago (solo para medioPago = LINK_DE_PAGO). */
    @Field("mercado_pago_preference_id")
    private String mercadoPagoPreferenceId;

    /** Id del pago ya confirmado en Mercado Pago, una vez que llega la notificación del webhook. */
    @Field("mercado_pago_payment_id")
    private String mercadoPagoPaymentId;

    @Field("fecha_creacion")
    private Instant fechaCreacion;

    @Field("fecha_actualizacion")
    private Instant fechaActualizacion;
}
