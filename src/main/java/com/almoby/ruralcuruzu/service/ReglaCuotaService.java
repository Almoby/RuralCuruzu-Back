package com.almoby.ruralcuruzu.service;

import java.util.List;

import com.almoby.ruralcuruzu.dto.request.ActualizarReglaCuotaRequest;
import com.almoby.ruralcuruzu.dto.response.ReglaCuotaActualizadaResponse;
import com.almoby.ruralcuruzu.dto.response.ReglaCuotaResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;

/**
 * Administración de las reglas de cuota por categoría de socio (documento,
 * sección 10.2). A lo sumo una regla por categoría: no hay "alta" separada
 * de "edición", todo pasa por {@link #actualizarRegla} (upsert).
 */
public interface ReglaCuotaService {

    List<ReglaCuotaResponse> listarReglas();

    ReglaCuotaResponse obtenerPorCategoria(CategoriaSocio categoria);

    ReglaCuotaActualizadaResponse actualizarRegla(CategoriaSocio categoria, ActualizarReglaCuotaRequest request);
}
