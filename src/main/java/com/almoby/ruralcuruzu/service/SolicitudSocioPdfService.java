package com.almoby.ruralcuruzu.service;

import com.almoby.ruralcuruzu.dto.response.SolicitudSocioResponse;

/**
 * Genera un PDF con el detalle completo de una solicitud de socio (pedido
 * del dueño del proyecto: poder descargar/imprimir la solicitud de
 * adhesión), para archivo físico o para adjuntar donde haga falta. No
 * decide reglas de negocio ni valida nada: solo maqueta los datos que ya
 * expone {@link SolicitudSocioResponse}.
 */
public interface SolicitudSocioPdfService {

    byte[] generarPdf(SolicitudSocioResponse solicitud);
}
