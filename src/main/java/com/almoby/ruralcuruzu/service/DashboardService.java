package com.almoby.ruralcuruzu.service;

import java.util.List;

import com.almoby.ruralcuruzu.dto.response.BeneficioMasUtilizadoResponse;
import com.almoby.ruralcuruzu.dto.response.CobranzaMensualPorCategoriaResponse;
import com.almoby.ruralcuruzu.dto.response.CobranzaMensualResponse;
import com.almoby.ruralcuruzu.dto.response.DashboardPrincipalResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoSociosResponse;
import com.almoby.ruralcuruzu.dto.response.IndicadoresPrincipalesResponse;
import com.almoby.ruralcuruzu.dto.response.SocioConDeudaResponse;
import com.almoby.ruralcuruzu.dto.response.UsoBeneficioPorComercioResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.TipoPersona;

/** Dashboard administrativo (documento, sección 7). Sin escritura: solo lecturas agregadas. */
public interface DashboardService {

    /**
     * Las 5 secciones del dashboard (7.1 a 7.5) más el desglose de deuda por
     * socio y de cobranza mensual por categoría, en una sola respuesta, para
     * que el front cargue toda la pantalla principal con una sola llamada.
     * {@code anio} es el de la cobranza mensual (secciones 7.2 y por
     * categoría); {@code categoria} y {@code tipoPersona} filtran el estado
     * de socios (sección 7.3). Cada sección también sigue disponible sola a
     * través de los métodos de abajo, reutilizados acá y por
     * {@link com.almoby.ruralcuruzu.service.DashboardExportService}.
     */
    DashboardPrincipalResponse obtenerDashboardPrincipal(int anio, CategoriaSocio categoria, TipoPersona tipoPersona);

    IndicadoresPrincipalesResponse obtenerIndicadoresPrincipales();

    List<CobranzaMensualResponse> obtenerCobranzaMensual(int anio);

    /**
     * Igual que obtenerCobranzaMensual, pero desglosando lo COBRADO (cuotas
     * PAGADA) por categoría de socio (ACTIVO/ADHERENTE), para el gráfico de
     * Reportes "Cuotas cobradas por mes de socios activos y adherentes". No
     * incluye lo pendiente (a diferencia de CobranzaMensualResponse), el
     * título del gráfico pide específicamente lo cobrado.
     */
    List<CobranzaMensualPorCategoriaResponse> obtenerCobranzaMensualPorCategoria(int anio);

    EstadoSociosResponse obtenerEstadoSocios(CategoriaSocio categoria, TipoPersona tipoPersona);

    /**
     * Un socio por fila para cada socio con al menos una cuota VENCIDA, con
     * el monto total adeudado: sirve tanto para el gráfico "Deuda acumulada
     * por socio" como para la lista "Socios con cuota vencida" (misma data,
     * dos formas de mostrarla en Reportes). Ordenado de mayor a menor deuda.
     */
    List<SocioConDeudaResponse> obtenerSociosConDeuda();

    List<UsoBeneficioPorComercioResponse> obtenerUsoBeneficiosPorComercio();

    /**
     * Ranking global de beneficios individuales por cantidad de usos del mes
     * actual (a diferencia de {@link #obtenerUsoBeneficiosPorComercio()}, que
     * agrupa por comercio). Ordenado de mayor a menor, sin límite.
     */
    List<BeneficioMasUtilizadoResponse> obtenerBeneficiosMasUtilizados();
}
