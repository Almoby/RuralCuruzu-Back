package com.almoby.ruralcuruzu.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.OrigenEjecucionCuotas;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de cada corrida de generación de cuotas (documento 10.2, paso 8:
 * "Registrar la ejecución"), tanto automática (cron mensual) como manual
 * (disparada por un admin). Es solo un log de auditoría: no participa de
 * ninguna regla de negocio.
 */
@Document(collection = "ejecuciones_generacion_cuotas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EjecucionGeneracionCuotas {

    @Id
    private String id;

    @Field("fecha_ejecucion")
    private Instant fechaEjecucion;

    @Field("origen")
    private OrigenEjecucionCuotas origen;

    @Field("periodo")
    private String periodo;

    @Field("cantidad_socios_activos")
    private int cantidadSociosActivos;

    @Field("cantidad_cuotas_generadas")
    private int cantidadCuotasGeneradas;

    @Field("cantidad_socios_omitidos")
    private int cantidadSociosOmitidos;

    /**
     * Socios ACTIVO a los que este mes no les tocaba generación (periodicidad
     * TRIMESTRAL/SEMESTRAL/ANUAL, y el mes del período no es uno de los suyos).
     * Se cuentan aparte de cantidadSociosOmitidos (que es por falta de regla de
     * cuota) para que el total siga cuadrando: activos = generadas + omitidos +
     * noLesCorrespondia.
     */
    @Field("cantidad_socios_no_les_correspondia")
    private int cantidadSociosNoLesCorrespondia;

    /** Null cuando el origen es AUTOMATICA. */
    @Field("admin_id")
    private String adminId;

    @Field("admin_nombre")
    private String adminNombre;
}
