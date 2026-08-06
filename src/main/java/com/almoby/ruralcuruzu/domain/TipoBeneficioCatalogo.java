package com.almoby.ruralcuruzu.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo administrable de tipos de beneficio/promoción (ej. "Descuento por
 * porcentaje", "2x1", "Gratis"). Reemplaza al viejo enum TipoBeneficio, fijo
 * en código: ahora el admin puede cargar tipos nuevos sin necesitar un
 * deploy, mismo patrón ya usado para {@link ReglaCuota}.
 *
 * {@link Beneficio} e {@link HistorialBeneficio} guardan una copia
 * denormalizada del nombre (y Beneficio, además, el id de referencia): borrar
 * o renombrar un tipo acá no altera los beneficios que ya lo tenían cargado,
 * solo afecta a los que se creen/editen de ahí en adelante.
 */
@Document(collection = "tipos_beneficio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoBeneficioCatalogo {

    @Id
    private String id;

    /** Identificador legible y estable (ej. "DESCUENTO_PORCENTAJE"), lo elige el admin al crear. */
    @Indexed(unique = true)
    @Field("codigo")
    private String codigo;

    /** Nombre para mostrar (ej. "Descuento por porcentaje"). */
    @Field("nombre")
    private String nombre;

    /** Solo los tipos activos aparecen en el dropdown para crear/editar un beneficio. */
    @Field("activo")
    private boolean activo;

    @Field("fecha_creacion")
    private Instant fechaCreacion;

    @Field("fecha_actualizacion")
    private Instant fechaActualizacion;
}
