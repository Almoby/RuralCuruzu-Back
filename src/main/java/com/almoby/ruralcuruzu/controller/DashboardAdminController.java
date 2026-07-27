package com.almoby.ruralcuruzu.controller;

import java.time.Year;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.response.CobranzaMensualResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoSociosResponse;
import com.almoby.ruralcuruzu.dto.response.IndicadoresPrincipalesResponse;
import com.almoby.ruralcuruzu.dto.response.UsoBeneficioPorComercioResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Dashboard administrativo (documento, sección 7). Todo bajo
 * /api/admin/dashboard/**, restringido a ROLE_ADMIN (SecurityConfig).
 * Todo de solo lectura: no hay altas/ediciones acá.
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.ADMIN_DASHBOARD_BASE)
@Tag(name = "Dashboard (Admin)", description = "Indicadores y gráficos operativos de la cooperativa. Solo ADMIN.")
@SecurityRequirement(name = "bearerAuth")
public class DashboardAdminController {

    private final DashboardService dashboardService;

    public DashboardAdminController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Indicadores principales (sección 7.1)",
            description = "Total de socios, socios por estado de cuota, comercios activos, facturación mensual, "
                    + "deuda acumulada y beneficios utilizados, entre otros datos de apoyo para cada tarjeta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicadores obtenidos correctamente")
    })
    @GetMapping("/indicadores")
    public ResponseEntity<IndicadoresPrincipalesResponse> obtenerIndicadores() {
        log.info("GET /api/admin/dashboard/indicadores");
        return ResponseEntity.ok(dashboardService.obtenerIndicadoresPrincipales());
    }

    @Operation(summary = "Gráfico de cobranza mensual (sección 7.2)",
            description = "Total cobrado y total pendiente por cada mes del año indicado (los 12 meses, con cero "
                    + "donde no haya datos). Sin año indicado, usa el año actual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos obtenidos correctamente")
    })
    @GetMapping("/cobranza-mensual")
    public ResponseEntity<List<CobranzaMensualResponse>> obtenerCobranzaMensual(
            @RequestParam(required = false) Integer año) {
        int añoConsultado = año != null ? año : Year.now().getValue();
        log.info("GET /api/admin/dashboard/cobranza-mensual - año={}", añoConsultado);
        return ResponseEntity.ok(dashboardService.obtenerCobranzaMensual(añoConsultado));
    }

    @Operation(summary = "Estado de socios (sección 7.3)",
            description = "Cantidad de socios al día, pendientes, vencidos e inactivos. Filtros opcionales por "
                    + "categoría (ACTIVO, ADHERENTE) y tipo de persona (FISICA, JURIDICA).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos obtenidos correctamente")
    })
    @GetMapping("/estado-socios")
    public ResponseEntity<EstadoSociosResponse> obtenerEstadoSocios(
            @RequestParam(required = false) CategoriaSocio categoria,
            @RequestParam(required = false) TipoPersona tipoPersona) {
        log.info("GET /api/admin/dashboard/estado-socios - categoria={} tipoPersona={}", categoria, tipoPersona);
        return ResponseEntity.ok(dashboardService.obtenerEstadoSocios(categoria, tipoPersona));
    }

    @Operation(summary = "Uso de beneficios por comercio (sección 7.4)",
            description = "Por cada comercio con al menos un uso: cantidad de beneficios utilizados, cantidad de "
                    + "socios únicos, la promoción más utilizada y el uso desglosado por período. Ordenado de "
                    + "mayor a menor cantidad de usos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos obtenidos correctamente")
    })
    @GetMapping("/uso-beneficios-por-comercio")
    public ResponseEntity<List<UsoBeneficioPorComercioResponse>> obtenerUsoBeneficiosPorComercio() {
        log.info("GET /api/admin/dashboard/uso-beneficios-por-comercio");
        return ResponseEntity.ok(dashboardService.obtenerUsoBeneficiosPorComercio());
    }
}
