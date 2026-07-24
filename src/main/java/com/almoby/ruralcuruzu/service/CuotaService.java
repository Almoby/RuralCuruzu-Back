package com.almoby.ruralcuruzu.service;

import java.util.List;

import com.almoby.ruralcuruzu.dto.request.AnularCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.InformarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RegistrarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RevisarPagoInformadoRequest;
import com.almoby.ruralcuruzu.dto.response.CuotaResponse;
import com.almoby.ruralcuruzu.dto.response.CuotaResumenResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoCuentaSocioResponse;
import com.almoby.ruralcuruzu.dto.response.GeneracionCuotasResponse;
import com.almoby.ruralcuruzu.dto.response.InformarPagoResponse;
import com.almoby.ruralcuruzu.dto.response.RegistrarPagoResponse;
import com.almoby.ruralcuruzu.enums.EstadoCuota;

/** Ver documento, sección 10 ("Gestión de cuotas"). */
public interface CuotaService {

    /**
     * Genera las cuotas del período indicado (o el mes actual si es null) para
     * todos los socios ACTIVO, tanto si la dispara el cron mensual como si la
     * dispara un admin manualmente (documento 10.2).
     */
    GeneracionCuotasResponse generarCuotas(String periodo, String adminId, String adminNombre);

    List<CuotaResumenResponse> listarCuotas(EstadoCuota estado, String socioId, String periodo);

    CuotaResponse obtenerCuotaPorId(String id);

    /** Registro manual de un pago hecho por el admin (documento 10.4). */
    RegistrarPagoResponse registrarPago(RegistrarPagoCuotaRequest request, String adminId, String adminNombre);

    /** El socio informa (autoservicio) que pagó una cuota propia. */
    InformarPagoResponse informarPago(String cuotaId, InformarPagoCuotaRequest request, String socioId);

    /** El admin aprueba o rechaza un pago informado por un socio (estado EN_REVISION). */
    CuotaResponse revisarPagoInformado(String cuotaId, RevisarPagoInformadoRequest request,
                                        String adminId, String adminNombre);

    CuotaResponse anularCuota(String id, AnularCuotaRequest request, String adminId, String adminNombre);

    EstadoCuentaSocioResponse obtenerEstadoCuentaSocio(String socioId);

    List<CuotaResumenResponse> listarCuotasDeSocio(String socioId);
}
