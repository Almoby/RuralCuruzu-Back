package com.almoby.ruralcuruzu.service;

/**
 * Exportación en PDF del reporte del dashboard administrativo (documento,
 * sección 7, botón "Exportar datos"). Reutiliza {@link DashboardService}
 * para no duplicar ninguna regla de cálculo: solo se encarga de maquetar en
 * PDF lo que esos métodos ya devuelven.
 */
public interface DashboardExportService {

    /**
     * Arma un único PDF con las 5 secciones del dashboard: indicadores
     * principales, cobranza mensual del año en curso, estado de socios, uso
     * de beneficios por comercio y ranking de beneficios más utilizados.
     */
    byte[] generarReportePdf();
}
