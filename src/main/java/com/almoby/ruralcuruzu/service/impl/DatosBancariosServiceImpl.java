package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.DatosBancarios;
import com.almoby.ruralcuruzu.dto.request.ActualizarDatosBancariosRequest;
import com.almoby.ruralcuruzu.dto.response.DatosBancariosActualizadosResponse;
import com.almoby.ruralcuruzu.dto.response.DatosBancariosResponse;
import com.almoby.ruralcuruzu.exception.DatosBancariosNoConfiguradosException;
import com.almoby.ruralcuruzu.repository.DatosBancariosRepository;
import com.almoby.ruralcuruzu.service.DatosBancariosService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DatosBancariosServiceImpl implements DatosBancariosService {

    private final DatosBancariosRepository datosBancariosRepository;

    public DatosBancariosServiceImpl(DatosBancariosRepository datosBancariosRepository) {
        this.datosBancariosRepository = datosBancariosRepository;
    }

    @Override
    public DatosBancariosResponse obtener() {
        return DatosBancariosResponse.from(buscarOFallar());
    }

    @Override
    public DatosBancariosActualizadosResponse actualizar(ActualizarDatosBancariosRequest request) {
        DatosBancarios datosBancarios = datosBancariosRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> DatosBancarios.builder().build());

        boolean esNuevo = datosBancarios.getId() == null;

        datosBancarios.setBanco(request.banco());
        datosBancarios.setCbu(request.cbu());
        datosBancarios.setAlias(request.alias());
        datosBancarios.setTitular(request.titular());
        datosBancarios.setCuit(request.cuit());
        datosBancarios.setFechaActualizacion(Instant.now());
        datosBancariosRepository.save(datosBancarios);

        log.info("Datos bancarios {} (banco={} alias={})", esNuevo ? "creados" : "actualizados",
                request.banco(), request.alias());

        return DatosBancariosActualizadosResponse.of(DatosBancariosResponse.from(datosBancarios), esNuevo);
    }

    private DatosBancarios buscarOFallar() {
        return datosBancariosRepository.findAll().stream()
                .findFirst()
                .orElseThrow(DatosBancariosNoConfiguradosException::new);
    }
}
