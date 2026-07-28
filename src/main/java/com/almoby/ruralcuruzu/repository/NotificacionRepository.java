package com.almoby.ruralcuruzu.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.Notificacion;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {

    /** La campanita de un usuario: más recientes primero. */
    List<Notificacion> findByDestinatarioIdOrderByFechaEnvioDesc(String destinatarioId);

    /** Para el contador de la campanita, sin traer todos los registros. */
    long countByDestinatarioIdAndLeidaFalse(String destinatarioId);
}
