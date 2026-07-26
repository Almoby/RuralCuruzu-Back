package com.almoby.ruralcuruzu.service;

import java.util.List;

import com.almoby.ruralcuruzu.dto.request.ActualizarBeneficioRequest;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoBeneficioRequest;
import com.almoby.ruralcuruzu.dto.request.CrearBeneficioRequest;
import com.almoby.ruralcuruzu.dto.request.ValidarBeneficioRequest;
import com.almoby.ruralcuruzu.dto.response.BeneficioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.BeneficioResponse;
import com.almoby.ruralcuruzu.dto.response.BeneficioResumenResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioConBeneficiosResponse;
import com.almoby.ruralcuruzu.dto.response.HistorialBeneficioResponse;
import com.almoby.ruralcuruzu.dto.response.ValidarBeneficioResponse;

/** Ver documento, sección 14 ("Beneficios y Comercios"). */
public interface BeneficioService {

    // ---------- self-service del comercio ----------

    BeneficioCreadoResponse crearBeneficio(String comercioId, CrearBeneficioRequest request);

    List<BeneficioResponse> listarBeneficiosDelComercio(String comercioId);

    BeneficioResponse obtenerBeneficioDelComercio(String comercioId, String beneficioId);

    BeneficioResponse actualizarBeneficio(String comercioId, String beneficioId, ActualizarBeneficioRequest request);

    BeneficioResponse cambiarEstadoBeneficio(String comercioId, String beneficioId, CambiarEstadoBeneficioRequest request);

    /** El comercio escanea el QR del socio y aplica uno de sus propios beneficios (documento 14, flujo de uso). */
    ValidarBeneficioResponse validarYUsarBeneficio(String comercioId, ValidarBeneficioRequest request);

    // ---------- consulta del socio ----------

    List<BeneficioResumenResponse> listarBeneficiosVigentes(String rubro, String busqueda);

    List<ComercioConBeneficiosResponse> listarComerciosConBeneficios(String rubro, String busqueda);

    List<HistorialBeneficioResponse> listarHistorialDeSocio(String socioId);
}
