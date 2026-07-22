package com.almoby.ruralcuruzu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuración explícita de MongoDB. Se arma el MongoClient "a mano" a partir de
 * la propiedad app.mongodb.uri (propia, no la magic property de Spring Boot) para
 * evitar depender de cómo cada versión de Spring Boot bindea spring.data.mongodb.uri.
 */
@Slf4j
@Configuration
public class MongoConfig {

    @Bean
    public MongoClient mongoClient(@Value("${app.mongodb.uri}") String uri) {
        String uriSaneada = sanearUri(uri);
        log.info("Conectando a Mongo con URI (enmascarada): {} [longitud original={}, longitud saneada={}]",
                enmascarar(uriSaneada), uri.length(), uriSaneada.length());
        return MongoClients.create(uriSaneada);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient,
                                                       @Value("${app.mongodb.uri}") String uri) {
        String nombreBaseDatos = new ConnectionString(sanearUri(uri)).getDatabase();
        return new SimpleMongoClientDatabaseFactory(mongoClient, nombreBaseDatos);
    }

    /**
     * Defensa contra errores de tipeo típicos al pegar la URI en variables de entorno:
     * espacios accidentales al principio/final, o una barra "/" sobrante después del
     * nombre de la base de datos (ej. ".../rural-curuzu-dev/" en vez de ".../rural-curuzu-dev").
     */
    private String sanearUri(String uri) {
        String limpia = uri.trim();
        int finDeRuta = limpia.indexOf('?');
        String base = finDeRuta >= 0 ? limpia.substring(0, finDeRuta) : limpia;
        String query = finDeRuta >= 0 ? limpia.substring(finDeRuta) : "";

        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        return base + query;
    }

    private String enmascarar(String uri) {
        return uri.replaceAll("://([^:]+):([^@]+)@", "://$1:****@");
    }
}
