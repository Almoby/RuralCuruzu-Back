package com.almoby.ruralcuruzu.dto.response;

import java.util.List;

/**
 * Todo lo que necesita la pantalla principal del dashboard admin (documento,
 * sección 7) en una sola llamada: las 5 secciones (7.1 a 7.5) que antes eran
 * 5 endpoints separados. Se agrupan acá por el mismo motivo que en el
 * dashboard de comercio: evitar 5 round-trips HTTP para una sola pantalla que
 * el front carga toda junta.
 */
public record DashboardPrincipalResponse(

        IndicadoresPrincipalesResponse indicadoresPrincipales,
        List<CobranzaMensualResponse> cobranzaMensual,
        EstadoSociosResponse estadoSocios,
        List<UsoBeneficioPorComercioResponse> usoBeneficiosPorComercio,
        List<BeneficioMasUtilizadoResponse> beneficiosMasUtilizados

) {
}
