package com.almoby.ruralcuruzu.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoUsoBeneficio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de un uso de beneficio (documento, secciones 14.4 y 19.3): se
 * crea al validar el QR del socio en el comercio (colección separada de
 * {@link Beneficio}, a pedido explícito). Todos los datos relevantes quedan
 * denormalizados acá porque es, ante todo, un comprobante histórico: si el
 * beneficio se edita o se pausa después, este registro no debe cambiar.
 *
 * Cuántas veces puede canjear un mismo socio el mismo beneficio lo decide el
 * comercio por beneficio (ver {@link Beneficio#limiteUsosPorSocio}, antes
 * era fijo en 1 para siempre). Por eso el índice de abajo NO es único: puede
 * haber más de un registro por par (socio, beneficio) si el límite lo
 * permite. Sigue siendo compuesto porque {@code BeneficioServiceImpl} lo
 * consulta todo el tiempo por ese mismo par (contar usos previos del socio
 * antes de autorizar un nuevo canje).
 */
@Document(collection = "historial_beneficios")
@CompoundIndex(name = "socio_beneficio_idx", def = "{'socio_id': 1, 'beneficio_id': 1}")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialBeneficio {

    @Id
    private String id;

    @Field("beneficio_id")
    private String beneficioId;

    @Field("beneficio_titulo")
    private String beneficioTitulo;

    /**
     * Nombre del tipo de beneficio al momento del canje (ej. "Descuento por
     * porcentaje"), copiado del catálogo administrable. Solo el nombre, sin el
     * id: este registro es un comprobante histórico, no necesita seguir
     * apuntando al catálogo (que puede cambiar o perder esa entrada después).
     */
    @Field("tipo_beneficio_nombre")
    private String tipoBeneficioNombre;

    @Field("valor")
    private String valor;

    @Field("comercio_id")
    private String comercioId;

    @Field("comercio_nombre")
    private String comercioNombre;

    /**
     * Usuario (cuenta de acceso) del comercio que realizó la validación
     * (documento, sección 15.6). Hoy cada Comercio tiene un único Usuario
     * vinculado (sin cuentas separadas por empleado), pero se guarda
     * explícito de todos modos: es lo que pide el documento, y deja el campo
     * listo si en el futuro un comercio tiene más de una cuenta de acceso.
     */
    @Field("usuario_comercio_id")
    private String usuarioComercioId;

    @Indexed
    @Field("socio_id")
    private String socioId;

    @Field("socio_numero_socio")
    private String socioNumeroSocio;

    @Field("socio_nombre")
    private String socioNombre;

    /** Denormalizado igual que el resto de los datos del socio: es el comprobante del momento del canje. */
    @Field("socio_categoria")
    private CategoriaSocio socioCategoria;

    @Field("monto_ahorro")
    private BigDecimal montoAhorro;

    @Field("estado")
    private EstadoUsoBeneficio estado;

    @Field("fecha_uso")
    private Instant fechaUso;

    @Field("motivo_anulacion")
    private String motivoAnulacion;
}
