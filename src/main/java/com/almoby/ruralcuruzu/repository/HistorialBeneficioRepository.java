package com.almoby.ruralcuruzu.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.HistorialBeneficio;

public interface HistorialBeneficioRepository extends MongoRepository<HistorialBeneficio, String> {

    List<HistorialBeneficio> findBySocioIdOrderByFechaUsoDesc(String socioId);

    boolean existsBySocioIdAndBeneficioId(String socioId, String beneficioId);
}
