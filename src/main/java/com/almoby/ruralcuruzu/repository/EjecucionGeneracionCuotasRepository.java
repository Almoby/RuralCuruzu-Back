package com.almoby.ruralcuruzu.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.EjecucionGeneracionCuotas;

public interface EjecucionGeneracionCuotasRepository extends MongoRepository<EjecucionGeneracionCuotas, String> {

    List<EjecucionGeneracionCuotas> findAllByOrderByFechaEjecucionDesc();
}
