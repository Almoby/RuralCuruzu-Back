package com.almoby.ruralcuruzu.service;

import java.util.List;

import com.almoby.ruralcuruzu.dto.request.CambiarEstadoTipoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.CrearTipoCuotaRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoTipoCuotaResponse;
import com.almoby.ruralcuruzu.dto.response.TipoCuotaCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.TipoCuotaResponse;
import com.almoby.ruralcuruzu.enums.EstadoTipoCuota;

/** Ver documento, sección 10.1 ("Tipos de cuota"). */
public interface TipoCuotaService {

    TipoCuotaCreadoResponse crearTipoCuota(CrearTipoCuotaRequest request);

    List<TipoCuotaResponse> listarTiposCuota(EstadoTipoCuota estado);

    TipoCuotaResponse obtenerTipoCuotaPorId(String id);

    CambiarEstadoTipoCuotaResponse cambiarEstadoTipoCuota(String id, CambiarEstadoTipoCuotaRequest request);
}
