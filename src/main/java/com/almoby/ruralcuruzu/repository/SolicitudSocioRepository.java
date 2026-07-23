package com.almoby.ruralcuruzu.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.enums.EstadoSolicitud;
import com.almoby.ruralcuruzu.domain.SolicitudSocio;

public interface SolicitudSocioRepository extends MongoRepository<SolicitudSocio, String> {

    Optional<SolicitudSocio> findByNumeroSolicitud(String numeroSolicitud);

    List<SolicitudSocio> findByEstado(EstadoSolicitud estado);

    /**
     * Solo bloquea el email si ya hay una solicitud "viva" (no rechazada/cancelada)
     * con ese email: una solicitud rechazada o cancelada no debería impedir volver
     * a solicitar el ingreso más adelante.
     */
    boolean existsByEmailIgnoreCaseAndEstadoIn(String email, Collection<EstadoSolicitud> estados);

    /**
     * "In" sobre un campo lista (documentos) hace un chequeo de intersección:
     * true si ALGUNO de los documentosCandidatos aparece en el array documentos
     * de alguna solicitud en esos estados. Así, un DNI distinto con el mismo
     * CUIL que una solicitud existente también queda bloqueado.
     */
    boolean existsByDocumentosInAndEstadoIn(Collection<String> documentosCandidatos, Collection<EstadoSolicitud> estados);
}
