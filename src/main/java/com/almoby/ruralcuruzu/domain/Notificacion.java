package com.almoby.ruralcuruzu.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.ResultadoNotificacion;
import com.almoby.ruralcuruzu.enums.TipoNotificacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de una notificación automática (documento, sección 29.3: "guardar
 * destinatario, tipo de correo, asunto, fecha, hora, resultado, intentos,
 * error si existiera"). Cumple doble función, sin necesidad de dos entidades
 * separadas: es el log de auditoría de cada correo que manda el sistema, y a
 * la vez el contenido de la campanita in-app del destinatario (campos
 * {@code leida}/{@code fechaLectura}), ya que ambos representan el mismo
 * evento de negocio visto desde dos canales distintos.
 *
 * {@code destinatarioId} es el {@link Usuario#getId()} resuelto por email al
 * momento de enviar (puede quedar null si ese email no corresponde a ningún
 * Usuario de la plataforma); es lo que permite filtrar "mis notificaciones"
 * sin tener que comparar por email en cada consulta.
 */
@Document(collection = "notificaciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    private String id;

    @Indexed
    @Field("destinatario_id")
    private String destinatarioId;

    @Field("destinatario_email")
    private String destinatarioEmail;

    @Field("destinatario_nombre")
    private String destinatarioNombre;

    @Field("tipo")
    private TipoNotificacion tipo;

    @Field("asunto")
    private String asunto;

    /** Texto corto para mostrar en la campanita (distinto del cuerpo completo del correo). */
    @Field("mensaje")
    private String mensaje;

    @Field("resultado")
    private ResultadoNotificacion resultado;

    /**
     * Cuántas veces se intentó mandar. Hoy siempre es 1 (no hay un mecanismo de
     * reintento automático todavía); el campo queda listo para cuando lo haya,
     * en vez de tener que migrar el esquema después.
     */
    @Field("intentos")
    @Builder.Default
    private int intentos = 1;

    /** Mensaje de la excepción que hizo fallar el envío, null si resultado es EXITOSO. */
    @Field("error")
    private String error;

    @Field("leida")
    @Builder.Default
    private boolean leida = false;

    @Field("fecha_lectura")
    private Instant fechaLectura;

    @Field("fecha_envio")
    private Instant fechaEnvio;
}
