package com.almoby.ruralcuruzu.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contador atómico genérico (patrón "counters collection" de Mongo) para
 * generar números de solicitud secuenciales sin depender de un autoincrement
 * nativo (que Mongo no tiene). Un documento por secuencia, identificado por
 * {@code _id} (ej. "solicitud_socio").
 */
@Document(collection = "contadores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contador {

    @Id
    private String id;

    @Field("valor")
    private long valor;
}
