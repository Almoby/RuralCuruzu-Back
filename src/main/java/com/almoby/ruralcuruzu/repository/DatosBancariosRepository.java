package com.almoby.ruralcuruzu.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.DatosBancarios;

/** Singleton: a lo sumo un documento (ver DatosBancarios). No hace falta ningún finder propio. */
public interface DatosBancariosRepository extends MongoRepository<DatosBancarios, String> {
}
