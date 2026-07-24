package com.almoby.ruralcuruzu.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.Cuota;
import com.almoby.ruralcuruzu.enums.EstadoCuota;

public interface CuotaRepository extends MongoRepository<Cuota, String> {

    List<Cuota> findBySocioId(String socioId);

    List<Cuota> findByEstado(EstadoCuota estado);

    boolean existsBySocioIdAndPeriodo(String socioId, String periodo);

    /** Usado por el job diario que marca VENCIDA a lo que quedó PENDIENTE. */
    List<Cuota> findByEstadoAndFechaVencimientoBefore(EstadoCuota estado, LocalDate fecha);
}
