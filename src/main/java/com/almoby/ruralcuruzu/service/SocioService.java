package com.almoby.ruralcuruzu.service;

import java.util.List;

import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.domain.SolicitudSocio;
import com.almoby.ruralcuruzu.dto.request.AltaManualSocioRequest;
import com.almoby.ruralcuruzu.dto.response.MiQrResponse;
import com.almoby.ruralcuruzu.dto.response.SocioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.SocioResponse;
import com.almoby.ruralcuruzu.dto.response.SocioResumenResponse;
import com.almoby.ruralcuruzu.enums.EstadoSocio;

/**
 * Alta y consulta de Socios. Hay dos formas de crear un Socio: aprobar una
 * {@link SolicitudSocio} (documento, sección 8.4) o el alta manual directa
 * del admin (documento, sección 9.5); esa lógica vive acá y no en
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
     * Alta manual de un socio por parte del admin (documento, sección 9.5):
     * a diferencia de crearSocioDesdeSolicitud, no hay ninguna SolicitudSocio
     * de por medio, el admin carga los datos directo y elige el estado
     * inicial (por defecto ACTIVO). Igual que en el resto del proyecto, se
     * crea también el Usuario con contraseña temporal y rol SOCIO, y se le
     * mandan las credenciales por correo.
     */
    SocioCreadoResponse crearSocioManual(AltaManualSocioRequest request, String adminId, String adminNombre);

    /**
     * Listado completo (sin paginación), opcionalmente filtrado por estado.
     * Usado, entre otras cosas, para poblar el select de socio del panel de Cuotas.
     */
    List<SocioResumenResponse> listarSocios(EstadoSocio estado);

    SocioResponse obtenerSocioPorId(String id);

    /**
     * "Mi QR" del socio (documento, sección 15): genera un token nuevo de
     * corta duración para mostrar en pantalla (módulo Beneficios), junto con
     * su estado vigente. Cada llamada devuelve un token distinto.
     */
    MiQrResponse obtenerMiQr(String socioId);
}
