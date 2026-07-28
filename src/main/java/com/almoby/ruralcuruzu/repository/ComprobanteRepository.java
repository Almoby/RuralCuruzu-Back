package com.almoby.ruralcuruzu.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.Comprobante;

public interface ComprobanteRepository extends MongoRepository<Comprobante, String> {

    Optional<Comprobante> findByPagoId(String pagoId);
}
