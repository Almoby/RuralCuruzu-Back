package com.almoby.ruralcuruzu.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.CategoriaSocio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Regla de cuota vigente para una categoría de socio (documento, sección
 * 10.2, paso "obtener importe"): nombre descriptivo, importe y día de
 * vencimiento. Como máximo una por categoría (índice único en
 * categoriaAplicable) — no hay historial de vigencias pasadas: cambiar el
 * importe acá afecta solo a las cuotas que se generen de ahí en adelante,
 * nunca a las ya generadas (cada Cuota ya guarda su propio importe al
 * crearse, así que no hace falta versionar esto).
 *
 * <p>Se administra desde el panel de admin (POST/GET/PUT
 * /api/admin/reglas-cuota) porque así lo pidió el dueño del proyecto: sin
 * pantalla no había forma de cargar o cambiar un precio sin tocar la base
 * a mano.
 */
@Document(collection = "reglas_cuota")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReglaCuota {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("categoria_aplicable")
    private CategoriaSocio categoriaAplicable;

    @Field("nombre")
    private String nombre;

    @Field("importe")
    private BigDecimal importe;

    @Field("dia_vencimiento")
    private int diaVencimiento;

    @Field("fecha_creacion")
    private Instant fechaCreacion;

    @Field("fecha_actualizacion")
    private Instant fechaActualizacion;
}
