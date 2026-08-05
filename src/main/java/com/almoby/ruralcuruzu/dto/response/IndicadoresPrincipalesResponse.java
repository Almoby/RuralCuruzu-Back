package com.almoby.ruralcuruzu.dto.response;

import java.math.BigDecimal;

/**
 * Tarjetas del dashboard administrativo (documento, sección 7.1). Los 8
 * indicadores pedidos por el documento son: totalSocios, sociosConCuotaAlDia,
 * sociosConCuotaPendiente, sociosConCuotaVencida, comerciosActivos,
 * facturacionMensual, deudaAcumulada y beneficiosUtilizados. El resto de los
 * campos son los datos adicionales que muestra el Figma en cada tarjeta
 * (variaciones, porcentajes, conteos relacionados).
 *
 * Definiciones acordadas:
 * - facturacionMensual: suma de TODAS las cuotas generadas para el período
 *   actual (facturado), sin importar si ya se pagaron, excepto las ANULADAS.
 * - deudaAcumulada: suma de las cuotas actualmente en estado VENCIDA
 *   únicamente (no incluye lo pendiente-no vencido).
 * - beneficiosUtilizados: usos DEL MES ACTUAL (HistorialBeneficio en estado
 *   USADO con fechaUso dentro del mes en curso). beneficiosUtilizadosHistoricoTotal
 *   es un dato adicional con el acumulado histórico completo.
 * - sociosActivos: cantidad de socios con EstadoSocio.ACTIVO (a diferencia de
 *   totalSocios, que cuenta TODOS los socios sin importar su estado).
 * - sociosNuevosEsteAnio: altas (fechaAlta) desde el 1 de enero del año en
 *   curso. Es la tarjeta "socios nuevos este año" de Reportes; distinta de
 *   sociosNuevosEsteMes, que sigue disponible para otras pantallas.
 */
public record IndicadoresPrincipalesResponse(

        long totalSocios,
        long sociosActivos,
        long sociosNuevosEsteMes,
        long sociosNuevosEsteAnio,

        long sociosConCuotaAlDia,
        double porcentajeAlDiaDelTotal,

        long sociosConCuotaPendiente,

        long sociosConCuotaVencida,

        long comerciosActivos,
        long promocionesActivas,

        BigDecimal facturacionMensual,
        BigDecimal variacionPorcentualFacturacionVsMesAnterior,

        BigDecimal deudaAcumulada,
        long sociosEnMora,

        long beneficiosUtilizados,
        long beneficiosUtilizadosHistoricoTotal

) {
}
