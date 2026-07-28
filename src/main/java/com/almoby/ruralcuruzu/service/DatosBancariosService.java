package com.almoby.ruralcuruzu.service;

import com.almoby.ruralcuruzu.dto.request.ActualizarDatosBancariosRequest;
import com.almoby.ruralcuruzu.dto.response.DatosBancariosActualizadosResponse;
import com.almoby.ruralcuruzu.dto.response.DatosBancariosResponse;

/**
 * Datos de la cuenta bancaria de la cooperativa (documento 10.4 / pantalla
 * "Mis Pagos"), para que el socio pueda transferir el pago de una cuota. Es
 * un singleton (ver {@link com.almoby.ruralcuruzu.domain.DatosBancarios}).
 */
public interface DatosBancariosService {

    /** @throws com.almoby.ruralcuruzu.exception.DatosBancariosNoConfiguradosException si ningún admin los cargó todavía. */
    DatosBancariosResponse obtener();

    /** Upsert: si todavía no existen, los crea; si ya existen, los actualiza. */
    DatosBancariosActualizadosResponse actualizar(ActualizarDatosBancariosRequest request);
}
