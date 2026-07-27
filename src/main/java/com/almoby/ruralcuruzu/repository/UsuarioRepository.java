package com.almoby.ruralcuruzu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.enums.Rol;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Usado para avisarle a todos los admins cuando hay algo nuevo para revisar. */
    List<Usuario> findByRol(Rol rol);
}
