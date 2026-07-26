package com.almoby.ruralcuruzu.domain;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.EstadoBeneficio;
import com.almoby.ruralcuruzu.enums.TipoBeneficio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Beneficio/promoción de un comercio (documento, sección 14). Lo crea y
 * administra el propio comercio (self-service, según el dueño del proyecto),
 * no el admin. Los datos del comercio se denormalizan (comercioNombre,
 * comercioRubro) para poder listar y filtrar sin resolver el comercio en
 * cada consulta, igual que en otros módulos de este proyecto.
 */
@Document(collection = "beneficios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Beneficio {

    @Id
    private String id;

    @Indexed
    @Field("comercio_id")
    private String comercioId;

    @Field("comercio_nombre")
    private String comercioNombre;

    @Field("comercio_rubro")
    private String comercioRubro;

    @Field("titulo")
    private String titulo;

    @Field("descripcion")
    private String descripcion;

    @Field("tipo")
    private TipoBeneficio tipo;

    /** Texto para el badge (ej. "15%", "2x1", "Gratis"): lo carga el comercio, no se calcula. */
    @Field("valor")
    private String valor;

    /** Null = vigente desde ya. */
    @Field("fecha_inicio_vigencia")
    private LocalDate fechaInicioVigencia;

    /** Null = sin fecha de vencimiento. */
    @Field("fecha_fin_vigencia")
    private LocalDate fechaFinVigencia;

    @Field("estado")
    private EstadoBeneficio estado;

    @Field("fecha_creacion")
    private Instant fechaCreacion;

    @Field("fecha_actualizacion")
    private Instant fechaActualizacion;

    /** Vigente hoy: ACTIVO y dentro del rango de fechas (si tiene). */
    public boolean estaVigenteHoy() {
        if (estado != EstadoBeneficio.ACTIVO) {
            return false;
        }
        LocalDate hoy = LocalDate.now();
        if (fechaInicioVigencia != null && hoy.isBefore(fechaInicioVigencia)) {
            return false;
        }
        return fechaFinVigencia == null || !hoy.isAfter(fechaFinVigencia);
    }
}
