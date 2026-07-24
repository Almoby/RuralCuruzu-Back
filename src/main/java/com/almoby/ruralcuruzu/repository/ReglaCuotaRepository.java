package com.almoby.ruralcuruzu.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.ReglaCuota;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;

public interface ReglaCuotaRepository extends MongoRepository<ReglaCuota, String> {

    Optional<ReglaCuota> findByCategoriaAplicable(CategoriaSocio categoriaAplicable);
}
