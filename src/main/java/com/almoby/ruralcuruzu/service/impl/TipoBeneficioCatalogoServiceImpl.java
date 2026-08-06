package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.TipoBeneficioCatalogo;
import com.almoby.ruralcuruzu.dto.request.ActualizarTipoBeneficioRequest;
import com.almoby.ruralcuruzu.dto.request.CrearTipoBeneficioRequest;
import com.almoby.ruralcuruzu.dto.response.TipoBeneficioActualizadoResponse;
import com.almoby.ruralcuruzu.dto.response.TipoBeneficioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.TipoBeneficioResponse;
import com.almoby.ruralcuruzu.exception.TipoBeneficioCodigoDuplicadoException;
import com.almoby.ruralcuruzu.exception.TipoBeneficioEnUsoException;
import com.almoby.ruralcuruzu.exception.TipoBeneficioNoEncontradoException;
import com.almoby.ruralcuruzu.repository.BeneficioRepository;
import com.almoby.ruralcuruzu.repository.TipoBeneficioCatalogoRepository;
import com.almoby.ruralcuruzu.service.TipoBeneficioCatalogoService;

import lombok.extern.slf4j.Slf4j;

/**
 * Catálogo administrable de tipos de beneficio. Reemplaza al enum
 * TipoBeneficio fijo (mismo motivo por el que en su momento se armó
 * ReglaCuota: cargar un tipo nuevo no debería necesitar un deploy).
 *
 * A diferencia de ReglaCuota (upsert por categoría, sin borrado), acá sí hay
 * alta/baja explícitas porque no hay un límite natural de "como mucho un
 * registro por clave": puede haber cualquier cantidad de tipos.
 */
@Slf4j
@Service
public class TipoBeneficioCatalogoServiceImpl implements TipoBeneficioCatalogoService {

    private final TipoBeneficioCatalogoRepository tipoBeneficioCatalogoRepository;
    private final BeneficioRepository beneficioRepository;

    public TipoBeneficioCatalogoServiceImpl(TipoBeneficioCatalogoRepository tipoBeneficioCatalogoRepository,
                                             BeneficioRepository beneficioRepository) {
        this.tipoBeneficioCatalogoRepository = tipoBeneficioCatalogoRepository;
        this.beneficioRepository = beneficioRepository;
    }

    @Override
    public List<TipoBeneficioResponse> listarTodos() {
        return tipoBeneficioCatalogoRepository.findAll().stream()
                .map(TipoBeneficioResponse::from)
                .toList();
    }

    @Override
    public List<TipoBeneficioResponse> listarActivos() {
        return tipoBeneficioCatalogoRepository.findByActivoTrue().stream()
                .map(TipoBeneficioResponse::from)
                .toList();
    }

    @Override
    public TipoBeneficioResponse obtenerPorId(String id) {
        return TipoBeneficioResponse.from(buscarOFallar(id));
    }

    @Override
    public TipoBeneficioCreadoResponse crear(CrearTipoBeneficioRequest request) {
        if (tipoBeneficioCatalogoRepository.existsByCodigo(request.codigo())) {
            throw new TipoBeneficioCodigoDuplicadoException(request.codigo());
        }

        Instant ahora = Instant.now();
        TipoBeneficioCatalogo tipo = TipoBeneficioCatalogo.builder()
                .codigo(request.codigo())
                .nombre(request.nombre())
                .activo(true)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .build();
        tipoBeneficioCatalogoRepository.save(tipo);

        log.info("Tipo de beneficio creado: id={} codigo={}", tipo.getId(), tipo.getCodigo());

        return TipoBeneficioCreadoResponse.of(TipoBeneficioResponse.from(tipo));
    }

    @Override
    public TipoBeneficioActualizadoResponse actualizar(String id, ActualizarTipoBeneficioRequest request) {
        TipoBeneficioCatalogo tipo = buscarOFallar(id);

        if (esNoVacio(request.nombre())) {
            tipo.setNombre(request.nombre());
        }
        if (request.activo() != null) {
            tipo.setActivo(request.activo());
        }
        tipo.setFechaActualizacion(Instant.now());
        tipoBeneficioCatalogoRepository.save(tipo);

        log.info("Tipo de beneficio id={} actualizado", id);

        return TipoBeneficioActualizadoResponse.of(TipoBeneficioResponse.from(tipo));
    }

    private boolean esNoVacio(String valor) {
        return valor != null && !valor.isBlank();
    }

    @Override
    public void eliminar(String id) {
        TipoBeneficioCatalogo tipo = buscarOFallar(id);

        if (beneficioRepository.existsByTipoBeneficioId(id)) {
            throw new TipoBeneficioEnUsoException(id);
        }

        tipoBeneficioCatalogoRepository.delete(tipo);
        log.info("Tipo de beneficio id={} eliminado", id);
    }

    private TipoBeneficioCatalogo buscarOFallar(String id) {
        return tipoBeneficioCatalogoRepository.findById(id)
                .orElseThrow(() -> new TipoBeneficioNoEncontradoException(id));
    }
}
