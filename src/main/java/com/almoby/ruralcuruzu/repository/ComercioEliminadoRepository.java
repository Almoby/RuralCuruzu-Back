package com.almoby.ruralcuruzu.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.ComercioEliminado;

public interface ComercioEliminadoRepository extends MongoRepository<ComercioEliminado, String> {

    /** Historial de bajas, más recientes primero. */
    List<ComercioEliminado> findAllByOrderByFechaBajaDesc();
}
