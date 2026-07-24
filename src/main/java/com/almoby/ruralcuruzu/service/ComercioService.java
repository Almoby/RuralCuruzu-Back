package com.almoby.ruralcuruzu.service;

import java.util.List;

import com.almoby.ruralcuruzu.dto.request.AltaComercioRequest;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoComercioRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoComercioResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioResumenResponse;
import com.almoby.ruralcuruzu.enums.EstadoComercio;

public interface ComercioService {

    /**
     * Da de alta un comercio (documento, sección 12.2), creando siempre su
     * Usuario con contraseña temporal y rol COMERCIO (sección 12.3).
     */
    ComercioCreadoResponse crearComercio(AltaComercioRequest request, String adminId, String adminNombre);

    /** Listado para el panel de admin, opcionalmente filtrado por estado. Sin paginación. */
    List<ComercioResumenResponse> listarComercios(EstadoComercio estado);

    ComercioResponse obtenerComercioPorId(String id);

    /** Cambia el estado de un comercio (documento, sección 12.4). Sin restricciones de transición. */
    CambiarEstadoComercioResponse cambiarEstadoComercio(String id, CambiarEstadoComercioRequest request);
}
