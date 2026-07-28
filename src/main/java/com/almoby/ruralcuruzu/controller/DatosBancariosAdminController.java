package com.almoby.ruralcuruzu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.request.ActualizarDatosBancariosRequest;
import com.almoby.ruralcuruzu.dto.response.DatosBancariosActualizadosResponse;
import com.almoby.ruralcuruzu.dto.response.DatosBancariosResponse;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.service.DatosBancariosService;

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
 * Administración de los datos bancarios de la cooperativa (documento, "Mis
 * Pagos" del socio). Todo bajo /api/admin/datos-bancarios/**, restringido a
 * ROLE_ADMIN (SecurityConfig). Es un singleton: no hay id en la URL.
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.ADMIN_DATOS_BANCARIOS_BASE)
@Tag(name = "Datos Bancarios (Admin)", description = "Cuenta bancaria de la cooperativa para recibir transferencias. Solo ADMIN.")
@SecurityRequirement(name = "bearerAuth")
public class DatosBancariosAdminController {

    private final DatosBancariosService datosBancariosService;

    public DatosBancariosAdminController(DatosBancariosService datosBancariosService) {
        this.datosBancariosService = datosBancariosService;
    }

    @Operation(summary = "Ver los datos bancarios configurados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos bancarios encontrados"),
            @ApiResponse(responseCode = "404", description = "Todavía no se configuraron",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<DatosBancariosResponse> obtener() {
        log.info("GET /api/admin/datos-bancarios");
        return ResponseEntity.ok(datosBancariosService.obtener());
    }

    @Operation(summary = "Crear o actualizar los datos bancarios",
            description = "Upsert: si todavía no hay datos cargados, los crea; si ya hay, los reemplaza por completo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos bancarios creados o actualizados correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping
    public ResponseEntity<DatosBancariosActualizadosResponse> actualizar(
            @Valid @RequestBody ActualizarDatosBancariosRequest request) {
        log.info("PUT /api/admin/datos-bancarios - banco={} alias={}", request.banco(), request.alias());
        return ResponseEntity.ok(datosBancariosService.actualizar(request));
    }
}
