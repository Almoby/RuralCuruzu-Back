package com.almoby.ruralcuruzu.service;

import java.util.List;

import com.almoby.ruralcuruzu.enums.EstadoSolicitud;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoSolicitudRequest;
import com.almoby.ruralcuruzu.dto.request.SolicitudSocioRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoSolicitudResponse;
import com.almoby.ruralcuruzu.dto.response.ObservacionAgregadaResponse;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioCreadaResponse;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioResponse;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioResumenResponse;

public interface SolicitudSocioService {

    /**
     * Crea una nueva solicitud de socio en estado PENDIENTE (ruta pública,
     * botón "Quiero ser socio"). Todavía no crea ningún Usuario habilitado.
     */
    SolicitudSocioCreadaResponse crearSolicitudSocio(SolicitudSocioRequest request);

    /** Listado para el panel de admin, opcionalmente filtrado por estado. Sin paginación. */
    List<SolicitudSocioResumenResponse> listarSolicitudesSocio(EstadoSolicitud estado);

    SolicitudSocioResponse obtenerSolicitudSocioPorNumero(String numeroSolicitud);

    /**
     * Cambia el estado de una solicitud (revisión, aprobación, rechazo,
     * cancelación), registrando quién lo hizo en el historial.
     */
    CambiarEstadoSolicitudResponse cambiarEstadoSolicitudSocio(String numeroSolicitud, CambiarEstadoSolicitudRequest request,
                                                                String adminId, String adminNombre);

    /**
     * Agrega una observación al historial sin cambiar el estado de la
     * solicitud (documento, sección 8.3: "agregar observaciones", "solicitar
     * correcciones", "solicitar documentación").
     */
    ObservacionAgregadaResponse agregarObservacion(String numeroSolicitud, String observacion,
                                                    String adminId, String adminNombre);
}
