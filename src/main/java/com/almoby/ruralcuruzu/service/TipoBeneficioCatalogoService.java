package com.almoby.ruralcuruzu.service;

import java.util.List;

import com.almoby.ruralcuruzu.dto.request.ActualizarTipoBeneficioRequest;
import com.almoby.ruralcuruzu.dto.request.CrearTipoBeneficioRequest;
import com.almoby.ruralcuruzu.dto.response.TipoBeneficioActualizadoResponse;
import com.almoby.ruralcuruzu.dto.response.TipoBeneficioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.TipoBeneficioResponse;

/**
 * Catálogo administrable de tipos de beneficio (reemplaza al viejo enum
 * TipoBeneficio fijo). Solo ADMIN administra el catálogo; comercio y admin
 * pueden listar los activos (para el dropdown al crear/editar un beneficio).
 */
public interface TipoBeneficioCatalogoService {

    List<TipoBeneficioResponse> listarTodos();

    /** Los que puede elegir un comercio hoy para un beneficio nuevo o editado. */
    List<TipoBeneficioResponse> listarActivos();

    TipoBeneficioResponse obtenerPorId(String id);

    TipoBeneficioCreadoResponse crear(CrearTipoBeneficioRequest request);

    TipoBeneficioActualizadoResponse actualizar(String id, ActualizarTipoBeneficioRequest request);

    /** Falla con TipoBeneficioEnUsoException si hay algún Beneficio con este tipoBeneficioId. */
    void eliminar(String id);
}
