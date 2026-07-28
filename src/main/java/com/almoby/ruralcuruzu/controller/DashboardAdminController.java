package com.almoby.ruralcuruzu.controller;

import java.time.LocalDate;
import java.time.Year;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.response.DashboardPrincipalResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.service.DashboardExportService;
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
    private final DashboardExportService dashboardExportService;

    public DashboardAdminController(DashboardService dashboardService, DashboardExportService dashboardExportService) {
        this.dashboardService = dashboardService;
        this.dashboardExportService = dashboardExportService;
    }

    @Operation(summary = "Dashboard principal (secciones 7.1 a 7.5)",
            description = "Las 5 secciones en una sola llamada: indicadores principales, cobranza mensual (12 "
                    + "meses del año indicado, o el año actual si no se indica), estado de socios (con filtros "
                    + "opcionales por categoría y tipo de persona), uso de beneficios por comercio, y ranking de "
                    + "beneficios más utilizados del mes en curso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos obtenidos correctamente")
    })
    @GetMapping
    public ResponseEntity<DashboardPrincipalResponse> obtenerDashboardPrincipal(
            @RequestParam(required = false) Integer año,
            @RequestParam(required = false) CategoriaSocio categoria,
            @RequestParam(required = false) TipoPersona tipoPersona) {
        int añoConsultado = año != null ? año : Year.now().getValue();
        log.info("GET /api/admin/dashboard - año={} categoria={} tipoPersona={}", añoConsultado, categoria, tipoPersona);
        return ResponseEntity.ok(dashboardService.obtenerDashboardPrincipal(añoConsultado, categoria, tipoPersona));
    }

    @Operation(summary = "Exportar el reporte completo en PDF",
            description = "Arma un único PDF con las 5 secciones del dashboard (indicadores principales, cobranza "
                    + "mensual del año en curso, estado de socios, uso de beneficios por comercio y ranking de "
                    + "beneficios más utilizados) para descargar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF generado correctamente")
    })
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportarReporte() {
        log.info("GET /api/admin/dashboard/exportar");
        byte[] pdf = dashboardExportService.generarReportePdf();

        String nombreArchivo = "reporte-rural-curuzu-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
