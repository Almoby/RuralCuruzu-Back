package com.almoby.ruralcuruzu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.enums.EstadoSocio;

public interface SocioRepository extends MongoRepository<Socio, String> {

    Optional<Socio> findByNumeroSocio(String numeroSocio);

    Optional<Socio> findByUsuarioId(String usuarioId);

    /** Usado por la generación de cuotas (documento 10.2, paso 1: "identificar socios activos"). */
    List<Socio> findByEstado(EstadoSocio estado);
}
