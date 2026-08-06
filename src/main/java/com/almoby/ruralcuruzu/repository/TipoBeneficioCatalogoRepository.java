package com.almoby.ruralcuruzu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.TipoBeneficioCatalogo;

public interface TipoBeneficioCatalogoRepository extends MongoRepository<TipoBeneficioCatalogo, String> {

    Optional<TipoBeneficioCatalogo> findByCodigo(String codigo);

    List<TipoBeneficioCatalogo> findByActivoTrue();

    boolean existsByCodigo(String codigo);
}
