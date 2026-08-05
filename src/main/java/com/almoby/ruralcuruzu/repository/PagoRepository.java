package com.almoby.ruralcuruzu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.Pago;
import com.almoby.ruralcuruzu.enums.EstadoPago;

public interface PagoRepository extends MongoRepository<Pago, String> {

    List<Pago> findByCuotaId(String cuotaId);

    List<Pago> findByCuotaIdIn(List<String> cuotaIds);

    List<Pago> findBySocioIdOrderByFechaCreacionDesc(String socioId);

    /** Usado por obtenerResumen: todos los pagos aprobados, para sumar totales sin ir cuota por cuota. */
    List<Pago> findByEstado(EstadoPago estado);

    Optional<Pago> findByMercadoPagoPreferenceId(String mercadoPagoPreferenceId);
}
