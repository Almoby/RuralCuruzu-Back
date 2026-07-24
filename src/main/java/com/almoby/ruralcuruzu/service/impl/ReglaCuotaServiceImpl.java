package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.ReglaCuota;
import com.almoby.ruralcuruzu.dto.request.ActualizarReglaCuotaRequest;
import com.almoby.ruralcuruzu.dto.response.ReglaCuotaActualizadaResponse;
import com.almoby.ruralcuruzu.dto.response.ReglaCuotaResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.exception.ReglaCuotaNoEncontradaException;
import com.almoby.ruralcuruzu.repository.ReglaCuotaRepository;
import com.almoby.ruralcuruzu.service.ReglaCuotaService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReglaCuotaServiceImpl implements ReglaCuotaService {

    private final ReglaCuotaRepository reglaCuotaRepository;

    public ReglaCuotaServiceImpl(ReglaCuotaRepository reglaCuotaRepository) {
        this.reglaCuotaRepository = reglaCuotaRepository;
    }

    @Override
    public List<ReglaCuotaResponse> listarReglas() {
        return reglaCuotaRepository.findAll().stream().map(ReglaCuotaResponse::from).toList();
    }

    @Override
    public ReglaCuotaResponse obtenerPorCategoria(CategoriaSocio categoria) {
        return ReglaCuotaResponse.from(buscarOFallar(categoria));
    }

    @Override
    public ReglaCuotaActualizadaResponse actualizarRegla(CategoriaSocio categoria, ActualizarReglaCuotaRequest request) {
        Instant ahora = Instant.now();

        ReglaCuota regla = reglaCuotaRepository.findByCategoriaAplicable(categoria)
                .orElseGet(() -> ReglaCuota.builder()
                        .categoriaAplicable(categoria)
                        .fechaCreacion(ahora)
                        .build());

        boolean esNueva = regla.getId() == null;

        regla.setNombre(request.nombre());
        regla.setImporte(request.importe());
        regla.setDiaVencimiento(request.diaVencimiento());
        regla.setFechaActualizacion(ahora);
        reglaCuotaRepository.save(regla);

        log.info("Regla de cuota categoria={} {} (importe={} diaVencimiento={})",
                categoria, esNueva ? "creada" : "actualizada", request.importe(), request.diaVencimiento());

        return ReglaCuotaActualizadaResponse.of(ReglaCuotaResponse.from(regla), esNueva);
    }

    private ReglaCuota buscarOFallar(CategoriaSocio categoria) {
        return reglaCuotaRepository.findByCategoriaAplicable(categoria)
                .orElseThrow(() -> new ReglaCuotaNoEncontradaException(categoria));
    }
}
