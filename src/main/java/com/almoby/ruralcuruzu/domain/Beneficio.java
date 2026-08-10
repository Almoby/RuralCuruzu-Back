package com.almoby.ruralcuruzu.domain;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.EstadoBeneficio;

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

    /**
     * Referencia a {@link TipoBeneficioCatalogo#getId()} (catálogo administrable
     * por el admin, reemplaza al viejo enum TipoBeneficio fijo). {@code
     * tipoBeneficioNombre} viaja denormalizado, igual que comercioNombre acá
     * arriba: si el admin renombra o desactiva el tipo después, este beneficio
     * sigue mostrando el nombre que tenía al crearse/editarse, hasta la
     * próxima edición.
     */
    @Field("tipo_beneficio_id")
    private String tipoBeneficioId;

    @Field("tipo_beneficio_nombre")
    private String tipoBeneficioNombre;

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

    /**
     * true si el último INACTIVO lo puso el comercio a propósito (PATCH
     * /estado), false si vino del job diario por vencimiento. Sin esto no
     * había forma de distinguir "lo pausé yo" de "se venció solo" una vez que
     * ambos casos dejan el mismo estado=INACTIVO — y sin esa distinción, editar
     * las fechas de un beneficio pausado a mano lo reactivaría por error (ver
     * BeneficioServiceImpl.actualizarBeneficio).
     */
    @Field("pausado_manualmente")
    private boolean pausadoManualmente;

    /**
     * Cuántas veces puede usar este beneficio el mismo socio, en total, para
     * siempre (no se resetea por día ni por mes: sigue siendo más parecido a
     * un cupón que a un descuento recurrente). Antes era fijo en 1; ahora lo
     * elige el comercio al crear o editar el beneficio.
     * - {@code null}: beneficios creados antes de esta mejora (el campo no
     *   existía). Se interpretan como 1 uso, para no cambiarles el
     *   comportamiento de la noche a la mañana a promociones ya publicadas.
     * - {@code 0}: sin límite, elegido a propósito por el comercio.
     * - Cualquier entero positivo: ese límite exacto.
     * Ver {@link #limiteUsosPorSocioEfectivo()} y {@link #puedeCanjear(long)}.
     */
    @Field("limite_usos_por_socio")
    private Integer limiteUsosPorSocio;

    @Field("fecha_creacion")
    private Instant fechaCreacion;

    @Field("fecha_actualizacion")
    private Instant fechaActualizacion;

    /** Vigente hoy: ACTIVO y dentro del rango de fechas (si tiene). */
    public boolean estaVigenteHoy() {
        return estado == EstadoBeneficio.ACTIVO && dentroDeVigenciaHoy();
    }

    /**
     * Chequeo puro de fechas (sin mirar el campo {@code estado}): true si hoy
     * cae dentro de [fechaInicioVigencia, fechaFinVigencia], considerando null
     * como "sin límite" en cada punta. Es la base para que el campo crudo
     * {@code estado} se auto-sincronice con las fechas tanto al crear/editar el
     * beneficio como en los jobs diarios (ver BeneficioServiceImpl), sin
     * depender de si en ese momento estaba ACTIVO o INACTIVO.
     */
    public boolean dentroDeVigenciaHoy() {
        LocalDate hoy = LocalDate.now();
        if (fechaInicioVigencia != null && hoy.isBefore(fechaInicioVigencia)) {
            return false;
        }
        return fechaFinVigencia == null || !hoy.isAfter(fechaFinVigencia);
    }

    /**
     * El estado que hay que MOSTRAR (front, reportes, etc.), a diferencia del
     * campo {@code estado} de arriba, que es el que el comercio puede tocar a
     * mano (pausar/reactivar) y que el job diario
     * (BeneficioServiceImpl.marcarBeneficiosVencidos) corrige recién a la
     * medianoche siguiente al vencimiento. Este método no depende de ese job:
     * calcula la vigencia en el momento, así que ya da INACTIVO desde el
     * instante exacto en que se cumple fechaFinVigencia, sin ventana de
     * espera. Todo consumidor externo (DTOs de respuesta) debería usar este
     * método, no el campo {@code estado} crudo.
     */
    public EstadoBeneficio estadoEfectivo() {
        return estaVigenteHoy() ? EstadoBeneficio.ACTIVO : EstadoBeneficio.INACTIVO;
    }

    /** Normaliza {@code limiteUsosPorSocio}: nunca null, {@code 1} es el default legado. */
    public int limiteUsosPorSocioEfectivo() {
        return limiteUsosPorSocio == null ? 1 : limiteUsosPorSocio;
    }

    /** true si, dados los usos previos de ESE socio en particular, todavía puede canjearlo de nuevo. */
    public boolean puedeCanjear(long usosPreviosDelSocio) {
        int limite = limiteUsosPorSocioEfectivo();
        return limite == 0 || usosPreviosDelSocio < limite;
    }

    /**
     * Cuántos usos le quedan a ESE socio en particular, dados sus usos totales
     * (incluyendo, si corresponde, el que se acaba de registrar). {@code null}
     * significa "ilimitado" (limiteUsosPorSocioEfectivo() == 0): no hay un
     * número de restantes que mostrar. Nunca negativo.
     */
    public Integer usosRestantes(long usosDelSocio) {
        int limite = limiteUsosPorSocioEfectivo();
        if (limite == 0) {
            return null;
        }
        return (int) Math.max(0, limite - usosDelSocio);
    }
}
