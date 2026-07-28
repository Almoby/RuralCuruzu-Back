package com.almoby.ruralcuruzu.service;

import com.almoby.ruralcuruzu.dto.response.EstadisticasComercioResponse;
import com.almoby.ruralcuruzu.dto.response.InicioComercioResponse;

/** "Inicio" y "Estadísticas" del portal de comercio: indicadores y gráficos propios. */
public interface ComercioDashboardService {

    /**
     * Indicadores (usos este mes, promociones activas, socios alcanzados,
     * validaciones de hoy) y la serie semanal (lunes a domingo, con 0 donde
     * no hubo usos), en una sola consulta por comercio.
     */
    InicioComercioResponse obtenerInicio(String comercioId);

    /**
     * Pantalla "Estadísticas": indicadores histórico + este mes, serie
     * mensual del año consultado (12 meses, con 0 donde no hubo usos), uso
     * por promoción de este mes y detalle de los últimos consumos.
     */
    EstadisticasComercioResponse obtenerEstadisticas(String comercioId, int anio);
}
