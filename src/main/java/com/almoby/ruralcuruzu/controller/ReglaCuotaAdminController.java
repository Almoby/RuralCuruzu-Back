package com.almoby.ruralcuruzu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.request.ActualizarReglaCuotaRequest;
import com.almoby.ruralcuruzu.dto.response.ReglaCuotaActualizadaResponse;
import com.almoby.ruralcuruzu.dto.response.ReglaCuotaResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.service.ReglaCuotaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Administración de reglas de cuota por categoría (documento, sección
 * 10.2). Todo bajo /api/admin/reglas-cuota/**, restringido a ROLE_ADMIN
 * (SecurityConfig). A lo sumo una regla por categoría (CategoriaSocio):
 * ACTIVO y ADHERENTE.
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.ADMIN_REGLAS_CUOTA_BASE)
@Tag(name = "Reglas de Cuota (Admin)", description = "Importe y día de vencimiento de la cuota por categoría de socio. Solo ADMIN.")
@SecurityRequirement(name = "bearerAuth")
public class ReglaCuotaAdminController {

    private final ReglaCuotaService reglaCuotaService;

    public ReglaCuotaAdminController(ReglaCuotaService reglaCuotaService) {
        this.reglaCuotaService = reglaCuotaService;
    }

    @Operation(summary = "Listar las reglas de cuota", description = "Como máximo dos (ACTIVO y ADHERENTE). Si una categoría todavía no tiene regla cargada, no aparece en la lista.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    @GetMapping
    public ResponseEntity<List<ReglaCuotaResponse>> listarReglas() {
        log.info("GET /api/admin/reglas-cuota");
        return ResponseEntity.ok(reglaCuotaService.listarReglas());
    }

    @Operation(summary = "Ver la regla de cuota de una categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Regla encontrada"),
            @ApiResponse(responseCode = "404", description = "Todavía no se cargó una regla para esa categoría",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{categoria}")
    public ResponseEntity<ReglaCuotaResponse> obtenerRegla(@PathVariable CategoriaSocio categoria) {
        log.info("GET /api/admin/reglas-cuota/{}", categoria);
        return ResponseEntity.ok(reglaCuotaService.obtenerPorCategoria(categoria));
    }

    @Operation(summary = "Crear o actualizar la regla de cuota de una categoría",
            description = "Upsert: si la categoría todavía no tiene regla, la crea; si ya tiene, actualiza importe, "
                    + "nombre y día de vencimiento. El cambio afecta solo a las cuotas que se generen de ahí en "
                    + "adelante, nunca a las ya generadas (cada cuota guarda su propio importe).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Regla creada o actualizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{categoria}")
    public ResponseEntity<ReglaCuotaActualizadaResponse> actualizarRegla(
            @PathVariable CategoriaSocio categoria,
            @Valid @RequestBody ActualizarReglaCuotaRequest request) {
        log.info("PUT /api/admin/reglas-cuota/{} - importe={} diaVencimiento={}",
                categoria, request.importe(), request.diaVencimiento());
        return ResponseEntity.ok(reglaCuotaService.actualizarRegla(categoria, request));
    }
}
