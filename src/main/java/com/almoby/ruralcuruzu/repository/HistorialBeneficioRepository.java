package com.almoby.ruralcuruzu.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.HistorialBeneficio;

public interface HistorialBeneficioRepository extends MongoRepository<HistorialBeneficio, String> {

    List<HistorialBeneficio> findBySocioIdOrderByFechaUsoDesc(String socioId);

    boolean existsBySocioIdAndBeneficioId(String socioId, String beneficioId);

    /** Usado en el panel admin: detalle de un comercio (consumos totales, usos por promoción). */
    List<HistorialBeneficio> findByComercioId(String comercioId);

    /** Usos de todos los beneficios del comercio desde una fecha (para el conteo "este mes" en lote). */
    List<HistorialBeneficio> findByComercioIdAndFechaUsoAfter(String comercioId, Instant desde);

    /** Usos de un beneficio puntual desde una fecha (detalle individual: crear/editar/pausar/consultar). */
    long countByBeneficioIdAndFechaUsoAfter(String beneficioId, Instant desde);
}
