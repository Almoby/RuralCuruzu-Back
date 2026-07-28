package com.almoby.ruralcuruzu.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de la cuenta bancaria de la cooperativa, para que el socio pueda
 * transferir el pago de su cuota (documento, sección 10.4 / pantalla "Mis
 * Pagos" del Figma). Es un singleton: a lo sumo un documento en toda la
 * colección (a diferencia de {@link ReglaCuota}, que tiene uno por
 * categoría) — el service siempre opera sobre ese único documento, sin
 * ningún campo discriminador.
 */
@Document(collection = "datos_bancarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatosBancarios {

    @Id
    private String id;

    @Field("banco")
    private String banco;

    @Field("cbu")
    private String cbu;

    @Field("alias")
    private String alias;

    @Field("titular")
    private String titular;

    @Field("cuit")
    private String cuit;

    @Field("fecha_actualizacion")
    private Instant fechaActualizacion;
}
