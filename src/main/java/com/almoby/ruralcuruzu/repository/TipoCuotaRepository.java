package com.almoby.ruralcuruzu.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.TipoCuota;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoTipoCuota;

public interface TipoCuotaRepository extends MongoRepository<TipoCuota, String> {

    List<TipoCuota> findByEstado(EstadoTipoCuota estado);

    /**
     * El tipo de cuota vigente para una categoría en un momento dado: el ACTIVO
     * con la fechaVigencia más reciente que ya haya llegado (permite cargar un
     * importe nuevo con vigencia futura sin que afecte la generación hasta esa fecha).
     */
    Optional<TipoCuota> findFirstByCategoriaAplicableAndEstadoAndFechaVigenciaLessThanEqualOrderByFechaVigenciaDesc(
            CategoriaSocio categoriaAplicable, EstadoTipoCuota estado, LocalDate fecha);
}
