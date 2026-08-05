package com.almoby.ruralcuruzu.dto.response;

import java.util.List;

/**
 * Todo lo que necesita la pantalla principal del dashboard admin (documento,
 * sección 7) en una sola llamada: las 5 secciones (7.1 a 7.5) que antes eran
 * 5 endpoints separados, más el desglose de deuda por socio y de cobranza
 * mensual por categoría que pidió el front. Se agrupan acá por el mismo
 * motivo que en el dashboard de comercio: evitar varios round-trips HTTP
 * para una sola pantalla que el front carga toda junta.
 */
public record DashboardPrincipalResponse(

        IndicadoresPrincipalesResponse indicadoresPrincipales,
        List<CobranzaMensualResponse> cobranzaMensual,
        List<CobranzaMensualPorCategoriaResponse> cobranzaMensualPorCategoria,
        EstadoSociosResponse estadoSocios,
        List<SocioConDeudaResponse> sociosConDeuda,
        List<UsoBeneficioPorComercioResponse> usoBeneficiosPorComercio,
        List<BeneficioMasUtilizadoResponse> beneficiosMasUtilizados

) {
}
