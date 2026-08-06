package com.almoby.ruralcuruzu.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.enums.EstadoBeneficio;

public interface BeneficioRepository extends MongoRepository<Beneficio, String> {

    List<Beneficio> findByComercioId(String comercioId);

    /** Usado para el listado del socio: se filtra la vigencia por fecha en el service. */
    List<Beneficio> findByEstado(EstadoBeneficio estado);

    /** Usado por el job diario que pasa a INACTIVO lo que ya venció (ver BeneficioServiceImpl). */
    List<Beneficio> findByEstadoAndFechaFinVigenciaBefore(EstadoBeneficio estado, LocalDate fecha);

    /**
     * Usado por el job diario que activa lo que ya empezó su vigencia (ver
     * BeneficioServiceImpl). Se excluyen los pausados a mano: pausadoManualmente=false
     * asegura que este job nunca reactive algo que el comercio dejó INACTIVO a propósito.
     */
    List<Beneficio> findByEstadoAndPausadoManualmenteFalseAndFechaInicioVigenciaLessThanEqual(
            EstadoBeneficio estado, LocalDate fecha);

    /** Usado al eliminar un comercio: sus beneficios se borran en cascada (ver ComercioServiceImpl.eliminarComercio). */
    void deleteByComercioId(String comercioId);

    /** Usado por TipoBeneficioCatalogoServiceImpl.eliminar para bloquear el borrado de un tipo en uso. */
    boolean existsByTipoBeneficioId(String tipoBeneficioId);
}
