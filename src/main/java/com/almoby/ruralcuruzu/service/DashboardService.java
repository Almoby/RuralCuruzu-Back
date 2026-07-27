package com.almoby.ruralcuruzu.service;

import java.util.List;

import com.almoby.ruralcuruzu.dto.response.BeneficioMasUtilizadoResponse;
import com.almoby.ruralcuruzu.dto.response.CobranzaMensualResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoSociosResponse;
import com.almoby.ruralcuruzu.dto.response.IndicadoresPrincipalesResponse;
import com.almoby.ruralcuruzu.dto.response.UsoBeneficioPorComercioResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.TipoPersona;

/** Dashboard administrativo (documento, sección 7). Sin escritura: solo lecturas agregadas. */
public interface DashboardService {

    IndicadoresPrincipalesResponse obtenerIndicadoresPrincipales();

    List<CobranzaMensualResponse> obtenerCobranzaMensual(int anio);

    EstadoSociosResponse obtenerEstadoSocios(CategoriaSocio categoria, TipoPersona tipoPersona);

    List<UsoBeneficioPorComercioResponse> obtenerUsoBeneficiosPorComercio();

    /**
     * Ranking global de beneficios individuales por cantidad de usos del mes
     * actual (a diferencia de {@link #obtenerUsoBeneficiosPorComercio()}, que
     * agrupa por comercio). Ordenado de mayor a menor, sin límite.
     */
    List<BeneficioMasUtilizadoResponse> obtenerBeneficiosMasUtilizados();
}
