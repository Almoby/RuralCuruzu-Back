package com.almoby.ruralcuruzu.service;

import java.util.List;

import com.almoby.ruralcuruzu.dto.request.ActualizarComercioParcialRequest;
import com.almoby.ruralcuruzu.dto.request.AltaComercioRequest;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoComercioRequest;
import com.almoby.ruralcuruzu.dto.request.EliminarComercioRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoComercioResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioActualizadoResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioEliminadoResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioResumenResponse;
import com.almoby.ruralcuruzu.dto.response.EliminarComercioResponse;
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

    /**
     * Edita un comercio campo por campo (PATCH): solo pisa los campos que
     * vengan con un valor no vacío en el request, el resto queda intacto. No
     * toca el estado ni la cuenta de acceso, salvo que cambie el correo
     * electrónico: en ese caso también actualiza el email de login del
     * Usuario vinculado.
     */
    ComercioActualizadoResponse actualizarComercioParcial(String id, ActualizarComercioParcialRequest request);

    /**
     * Borrado físico real (a diferencia de cambiar el estado a
     * DADO_DE_BAJA): el Comercio deja de existir, pero antes se guarda una
     * copia de auditoría en ComercioEliminado. En cascada, borra también sus
     * Beneficios y su cuenta de acceso (Usuario), para que no queden
     * huérfanos ni pueda seguir logueándose.
     */
    EliminarComercioResponse eliminarComercio(String id, EliminarComercioRequest request, String adminId, String adminNombre);

    /** Historial de comercios eliminados (auditoría), más recientes primero. */
    List<ComercioEliminadoResponse> listarComerciosEliminados();
}
