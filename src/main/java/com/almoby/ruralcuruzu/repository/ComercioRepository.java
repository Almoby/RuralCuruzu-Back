package com.almoby.ruralcuruzu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.Comercio;
import com.almoby.ruralcuruzu.enums.EstadoComercio;

public interface ComercioRepository extends MongoRepository<Comercio, String> {

    boolean existsByCuit(String cuit);

    boolean existsByCorreoElectronicoIgnoreCase(String correoElectronico);

    List<Comercio> findByEstado(EstadoComercio estado);

    Optional<Comercio> findByUsuarioId(String usuarioId);
}
