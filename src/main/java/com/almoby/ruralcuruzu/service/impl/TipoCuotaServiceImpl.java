package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.TipoCuota;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoTipoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.CrearTipoCuotaRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoTipoCuotaResponse;
import com.almoby.ruralcuruzu.dto.response.TipoCuotaCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.TipoCuotaResponse;
import com.almoby.ruralcuruzu.enums.EstadoTipoCuota;
import com.almoby.ruralcuruzu.exception.TipoCuotaNoEncontradoException;
import com.almoby.ruralcuruzu.repository.TipoCuotaRepository;
import com.almoby.ruralcuruzu.service.TipoCuotaService;

import lombok.extern.slf4j.Slf4j;

/**
 * Ver documento, sección 10.1 ("Tipos de cuota"). CuotaServiceImpl usa
 * TipoCuotaRepository directamente (no este service) para resolver, al
 * generar cuotas, el tipo vigente de cada categoría.
 */
@Slf4j
@Service
public class TipoCuotaServiceImpl implements TipoCuotaService {

    private final TipoCuotaRepository tipoCuotaRepository;

    public TipoCuotaServiceImpl(TipoCuotaRepository tipoCuotaRepository) {
        this.tipoCuotaRepository = tipoCuotaRepository;
    }

    @Override
    public TipoCuotaCreadoResponse crearTipoCuota(CrearTipoCuotaRequest request) {
        Instant ahora = Instant.now();
        TipoCuota tipoCuota = TipoCuota.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .categoriaAplicable(request.categoriaAplicable())
                .importe(request.importe())
                .fechaVigencia(request.fechaVigencia())
                .diaVencimiento(request.diaVencimiento())
                .estado(request.estado() != null ? request.estado() : EstadoTipoCuota.ACTIVO)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .build();

        tipoCuotaRepository.save(tipoCuota);
        log.info("Tipo de cuota creado id={} nombre={} categoria={}",
                tipoCuota.getId(), tipoCuota.getNombre(), tipoCuota.getCategoriaAplicable());

        return TipoCuotaCreadoResponse.of(TipoCuotaResponse.from(tipoCuota));
    }

    @Override
    public List<TipoCuotaResponse> listarTiposCuota(EstadoTipoCuota estado) {
        List<TipoCuota> tipos = estado != null
                ? tipoCuotaRepository.findByEstado(estado)
                : tipoCuotaRepository.findAll();

        return tipos.stream().map(TipoCuotaResponse::from).toList();
    }

    @Override
    public TipoCuotaResponse obtenerTipoCuotaPorId(String id) {
        return TipoCuotaResponse.from(buscarOFallar(id));
    }

    @Override
    public CambiarEstadoTipoCuotaResponse cambiarEstadoTipoCuota(String id, CambiarEstadoTipoCuotaRequest request) {
        TipoCuota tipoCuota = buscarOFallar(id);

        tipoCuota.setEstado(request.nuevoEstado());
        tipoCuota.setFechaActualizacion(Instant.now());
        tipoCuotaRepository.save(tipoCuota);

        log.info("Tipo de cuota id={} pasó a estado={}", id, request.nuevoEstado());

        return CambiarEstadoTipoCuotaResponse.of(id, request.nuevoEstado());
    }

    private TipoCuota buscarOFallar(String id) {
        return tipoCuotaRepository.findById(id)
                .orElseThrow(() -> new TipoCuotaNoEncontradoException(id));
    }
}
