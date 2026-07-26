package com.almoby.ruralcuruzu.service;

import java.util.List;

import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.domain.SolicitudSocio;
import com.almoby.ruralcuruzu.dto.response.MiQrResponse;
import com.almoby.ruralcuruzu.dto.response.SocioResponse;
import com.almoby.ruralcuruzu.dto.response.SocioResumenResponse;
import com.almoby.ruralcuruzu.enums.EstadoSocio;

/**
 * Alta y consulta de Socios. Hoy la única forma de crear un Socio es aprobar una
 * {@link SolicitudSocio} (documento, sección 8.4); esa lógica vive acá y no en
 * SolicitudSocioService para no mezclar responsabilidades de dos módulos distintos.
 */
public interface SocioService {

    /**
     * Crea el Socio a partir de una solicitud recién aprobada: asigna número
     * de socio y categoría, copia los datos personales de la solicitud, crea
     * el Usuario con contraseña temporal y rol SOCIO, y manda las credenciales
     * por correo (documento, sección 8.4, pasos 1 a 9).
     */
    Socio crearSocioDesdeSolicitud(SolicitudSocio solicitud, String adminId, String adminNombre);

    /**
     * Listado completo (sin paginación), opcionalmente filtrado por estado.
     * Usado, entre otras cosas, para poblar el select de socio del panel de Cuotas.
     */
    List<SocioResumenResponse> listarSocios(EstadoSocio estado);

    SocioResponse obtenerSocioPorId(String id);

    /** El código QR propio de un socio (módulo Beneficios), para mostrar en pantalla. */
    MiQrResponse obtenerMiQr(String socioId);
}
