package com.almoby.ruralcuruzu.service;

import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.dto.response.EstadoQrResponse;

/**
 * Calcula el estado vigente del QR de un socio (documento, secciones 15.2 y
 * 15.3). Lo comparten "Mi QR" del socio (sección 15.4, vía SocioService) y el
 * canje de beneficios del comercio (sección 15.6, vía BeneficioService), para
 * no duplicar la misma regla de negocio en dos lugares distintos.
 */
public interface EstadoQrService {

    /** Estado, mensaje informativo y fechas del QR de este socio, en este momento. */
    EstadoQrResponse calcularEstado(Socio socio);

    /**
     * Lanza QrNoValidoException si el QR del socio no está ACTIVO ahora
     * mismo (documento 15.2): usarlo como guarda antes de aplicar cualquier
     * beneficio.
     */
    void validarQrActivo(Socio socio);
}
