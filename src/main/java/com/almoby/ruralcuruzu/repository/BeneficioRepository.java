package com.almoby.ruralcuruzu.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.enums.EstadoBeneficio;

public interface BeneficioRepository extends MongoRepository<Beneficio, String> {

    List<Beneficio> findByComercioId(String comercioId);

    /** Usado para el listado del socio: se filtra la vigencia por fecha en el service. */
    List<Beneficio> findByEstado(EstadoBeneficio estado);
}
