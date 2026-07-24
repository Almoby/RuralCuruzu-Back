package com.almoby.ruralcuruzu.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.MedioPago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de un pago de cuota (documento, sección 10.4), embebido dentro de
 * {@link Cuota}. Se usa tanto para el registro manual del admin como para el
 * pago que informa un socio por autoservicio (en ese caso {@link #informadoPorSocio}
 * queda en true y los campos "registradoPorAdmin*" quedan null hasta que un
 * admin lo revise y apruebe).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatosPago {

    @Field("fecha_pago")
    private Instant fechaPago;

    @Field("importe")
    private BigDecimal importe;

    @Field("medio_pago")
    private MedioPago medioPago;

    @Field("comprobante")
    private String comprobante;

    @Field("observacion")
    private String observacion;

    /** true si lo cargó el propio socio (autoservicio), false si lo cargó un admin. */
    @Field("informado_por_socio")
    private boolean informadoPorSocio;

    @Field("registrado_por_admin_id")
    private String registradoPorAdminId;

    @Field("registrado_por_admin_nombre")
    private String registradoPorAdminNombre;
}
