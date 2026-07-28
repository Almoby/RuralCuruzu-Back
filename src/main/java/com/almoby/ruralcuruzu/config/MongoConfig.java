package com.almoby.ruralcuruzu.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuración explícita de MongoDB. Se arma el MongoClient "a mano" a partir de
 * la propiedad app.mongodb.uri (propia, no la magic property de Spring Boot) para
 * evitar depender de cómo cada versión de Spring Boot bindea spring.data.mongodb.uri.
 *
 * IMPORTANTE: los dos beans están condicionados a que NO exista ya un bean
 * MongoConnectionDetails. En los tests, TestcontainersConfiguration levanta un
 * Mongo real en Docker y lo registra automáticamente como MongoConnectionDetails
 * (vía @ServiceConnection); en ese caso hay que dejar que la autoconfiguración
 * de Spring Boot arme el cliente apuntando a ese contenedor, no forzar el
 * nuestro (que exigiría MONGODB_URI y apuntaría, si existiera, al Atlas real).
 * Fuera de los tests no hay ningún MongoConnectionDetails registrado, así que
 * estos beans se siguen usando exactamente igual que antes.
 */
@Slf4j
@Configuration
public class MongoConfig {

    /**
     * Sin esto, Spring Data guarda un LocalDate convirtiéndolo a un instante
     * (BSON Date) con la zona horaria por defecto de la JVM que esté corriendo
     * en ese momento — y lo vuelve a convertir a LocalDate, al leerlo, con la
     * zona horaria de la JVM que lo lea. Si ambas JVMs no coinciden (ej. se
     * escribió desde una máquina en horario de Argentina y se lee desde un
     * servidor en UTC), la fecha puede correrse un día para adelante o para
     * atrás sin ningún error visible: es la causa más probable de que un
     * beneficio con fecha_fin_vigencia "hoy" aparezca como no vigente, o de
     * que una cuota parezca vencida un día antes o después de lo real.
     * Guardar el LocalDate como texto ISO ("2026-07-27") en vez de como
     * instante elimina la zona horaria de la ecuación por completo.
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new LocalDateToStringConverter(), new StringToLocalDateConverter()));
    }

    @WritingConverter
    static class LocalDateToStringConverter implements Converter<LocalDate, String> {
        @Override
        public String convert(LocalDate source) {
            return source.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }

    @ReadingConverter
    static class StringToLocalDateConverter implements Converter<String, LocalDate> {
        @Override
        public LocalDate convert(String source) {
            return LocalDate.parse(source, DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }

    @Bean
    @ConditionalOnMissingBean(MongoConnectionDetails.class)
    public MongoClient mongoClient(@Value("${app.mongodb.uri}") String uri) {
        String uriSaneada = sanearUri(uri);
        log.info("Conectando a Mongo con URI (enmascarada): {} [longitud original={}, longitud saneada={}]",
                enmascarar(uriSaneada), uri.length(), uriSaneada.length());
        return MongoClients.create(uriSaneada);
    }

    @Bean
    @ConditionalOnMissingBean(MongoConnectionDetails.class)
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
