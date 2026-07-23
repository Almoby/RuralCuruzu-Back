package com.almoby.ruralcuruzu.domain;

import java.time.Instant;

import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.EstadoSolicitud;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una entrada del historial de auditoría de una {@link SolicitudSocio}.
 * Documento 5.5: "cada cambio deberá registrar fecha, hora, administrador
 * responsable, observación y motivo (cuando corresponda)". Se guarda un
 * registro también para la creación inicial (estadoAnterior == null).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CambioEstadoSolicitud {

    @Field("estado_anterior")
    private EstadoSolicitud estadoAnterior;

    @Field("estado_nuevo")
    private EstadoSolicitud estadoNuevo;

    /**
     * Fecha y hora del cambio (incluye ambas: Instant ya es una marca de
     * tiempo completa, no hace falta separarlas en dos campos).
     */
    @Field("fecha_hora")
    private Instant fechaHora;

    /**
     * Null cuando el cambio lo generó el propio solicitante (alta inicial).
     */
    @Field("admin_responsable_id")
    private String adminResponsableId;

    @Field("admin_responsable_nombre")
    private String adminResponsableNombre;

    @Field("observacion")
    private String observacion;

    @Field("motivo")
    private String motivo;
}
