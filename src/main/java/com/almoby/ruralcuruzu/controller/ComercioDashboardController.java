package com.almoby.ruralcuruzu.controller;

import java.time.Year;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.response.EstadisticasComercioResponse;
import com.almoby.ruralcuruzu.dto.response.InicioComercioResponse;
import com.almoby.ruralcuruzu.security.AuthenticatedUser;
import com.almoby.ruralcuruzu.service.ComercioDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * "Inicio" del portal de comercio: indicadores propios y uso semanal de sus
 * beneficios (equivalente reducido del dashboard admin, sección 7, pero
 * acotado a los datos del propio comercio). Todo bajo /api/comercio/dashboard/**,
 * restringido a ROLE_COMERCIO (SecurityConfig). El comercioId nunca viaja en
 * la URL: se resuelve siempre desde el usuario autenticado.
 *
 * <p>Un único endpoint para toda la pantalla (en vez de uno por tarjeta o
 * por gráfico): evita un round-trip HTTP extra por algo que ya se resuelve
 * con las mismas consultas a la base.
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.COMERCIO_DASHBOARD_BASE)
@Tag(name = "Dashboard (Comercio)", description = "Indicadores propios del comercio autenticado. Solo COMERCIO.")
@SecurityRequirement(name = "bearerAuth")
public class ComercioDashboardController {

    private final ComercioDashboardService comercioDashboardService;

    public ComercioDashboardController(ComercioDashboardService comercioDashboardService) {
        this.comercioDashboardService = comercioDashboardService;
    }

    @Operation(summary = "Datos del Inicio",
            description = "El estado de la propia cuenta del comercio (ACTIVO, INACTIVO, SUSPENDIDO, "
                    + "DADO_DE_BAJA), indicadores (usos este mes, promociones activas, socios alcanzados "
                    + "histórico, validaciones de hoy) y la serie semanal de usos (lunes a domingo, con 0 en los "
                    + "días sin usos), en una sola llamada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos obtenidos correctamente")
    })
    @GetMapping
    public ResponseEntity<InicioComercioResponse> obtenerInicio(@AuthenticationPrincipal AuthenticatedUser comercio) {
        String comercioId = comercio.usuario().getRefId();
        log.info("GET /api/comercio/dashboard - comercioId={}", comercioId);
        return ResponseEntity.ok(comercioDashboardService.obtenerInicio(comercioId));
    }

    @Operation(summary = "Datos de Estadísticas",
            description = "Indicadores histórico (usos totales, socios únicos, promociones activas, usos este "
                    + "mes), serie mensual del año indicado (o el actual si no se indica, 12 meses con 0 en los "
                    + "que no tuvieron usos), uso por promoción de este mes y detalle de los últimos consumos, "
                    + "en una sola llamada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos obtenidos correctamente")
    })
    @GetMapping("/estadisticas")
    public ResponseEntity<EstadisticasComercioResponse> obtenerEstadisticas(
            @AuthenticationPrincipal AuthenticatedUser comercio,
            @RequestParam(required = false) Integer año) {
        String comercioId = comercio.usuario().getRefId();
        int añoConsultado = año != null ? año : Year.now().getValue();
        log.info("GET /api/comercio/dashboard/estadisticas - comercioId={} año={}", comercioId, añoConsultado);
        return ResponseEntity.ok(comercioDashboardService.obtenerEstadisticas(comercioId, añoConsultado));
    }
}
