package com.almoby.ruralcuruzu.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.OrigenComprobante;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * El comprobante de un {@link Pago}, como su propia entidad de base de datos
 * (documento, sección 10.4: la pantalla "Comprobantes" necesita listar y
 * descargar uno por cada pago, con su propia metadata) — no un campo suelto
 * dentro de Pago. Relación 1:1 con un Pago.
 *
 * Hay dos orígenes posibles (ver {@link OrigenComprobante}): el archivo real
 * que el socio adjuntó al informar una transferencia, o una constancia en PDF
 * que el sistema genera la primera vez que hace falta para un pago sin
 * archivo real (registrado por un admin en ventanilla, o pagado por Mercado
 * Pago) una vez que ya está APROBADO. En ambos casos termina siendo un
 * archivo real en disco (ver AlmacenamientoComprobantesService), así que la
 * descarga es siempre la misma operación sin importar el origen.
 */
@Document(collection = "comprobantes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comprobante {

    @Id
    private String id;

    /** Uno por pago: no debería haber dos comprobantes para el mismo Pago. */
    @Indexed(unique = true)
    @Field("pago_id")
    private String pagoId;

    @Field("cuota_id")
    private String cuotaId;

    @Indexed
    @Field("socio_id")
    private String socioId;

    @Field("origen")
    private OrigenComprobante origen;

    /** Nombre para mostrarle al socio al descargar (sin el prefijo UUID con el que se guarda en disco). */
    @Field("nombre_archivo")
    private String nombreArchivo;

    /** Ruta relativa dentro del directorio de comprobantes (ver AlmacenamientoComprobantesService). */
    @Field("ruta")
    private String ruta;

    /** Puede ser null para comprobantes migrados desde el dato legacy Pago.comprobanteRuta. */
    @Field("content_type")
    private String contentType;

    @Field("tamanio_bytes")
    private long tamanioBytes;

    @Field("fecha_creacion")
    private Instant fechaCreacion;
}
