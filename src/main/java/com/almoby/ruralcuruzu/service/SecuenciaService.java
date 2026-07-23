package com.almoby.ruralcuruzu.service;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.Contador;

/**
 * Genera números secuenciales atómicos (ej. números de solicitud) usando el
 * patrón "counters collection" de Mongo: un findAndModify con $inc es atómico
 * a nivel de documento, así que no hay riesgo de que dos solicitudes
 * concurrentes reciban el mismo número (a diferencia de leer-incrementar-guardar).
 */
@Service
public class SecuenciaService {

    private final MongoTemplate mongoTemplate;

    public SecuenciaService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Devuelve el siguiente valor de la secuencia identificada por {@code nombre},
     * creándola en 1 si todavía no existe (upsert).
     */
    public long siguienteValor(String nombre) {
        Contador contador = mongoTemplate.findAndModify(
                query(where("_id").is(nombre)),
                new Update().inc("valor", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Contador.class);
        return contador != null ? contador.getValor() : 1L;
    }
}
