package com.almoby.ruralcuruzu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoTipoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.CrearTipoCuotaRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoTipoCuotaResponse;
import com.almoby.ruralcuruzu.dto.response.TipoCuotaCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.TipoCuotaResponse;
import com.almoby.ruralcuruzu.enums.EstadoTipoCuota;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.service.TipoCuotaService;

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
 * Administración de tipos de cuota (documento, sección 10.1). Todo bajo
 * /api/admin/tipos-cuota/**, restringido a ROLE_ADMIN (SecurityConfig).
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.ADMIN_TIPOS_CUOTA_BASE)
@Tag(name = "Tipos de Cuota (Admin)", description = "Alta, listado, detalle y cambio de estado de tipos de cuota. Solo ADMIN.")
@SecurityRequirement(name = "bearerAuth")
public class TipoCuotaAdminController {

    private final TipoCuotaService tipoCuotaService;

    public TipoCuotaAdminController(TipoCuotaService tipoCuotaService) {
        this.tipoCuotaService = tipoCuotaService;
    }

    @Operation(summary = "Crear un tipo de cuota",
            description = "Ej: 'Cuota de socio activo', 'Cuota anual'. Define importe, categoría a la que aplica, "
                    + "fecha de vigencia y día de vencimiento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo de cuota creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TipoCuotaCreadoResponse> crearTipoCuota(@Valid @RequestBody CrearTipoCuotaRequest request) {
        log.info("POST /api/admin/tipos-cuota - nombre={} categoria={}", request.nombre(), request.categoriaAplicable());

        TipoCuotaCreadoResponse response = tipoCuotaService.crearTipoCuota(request);

        log.info("POST /api/admin/tipos-cuota - creado id={}", response.tipoCuota().id());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar tipos de cuota", description = "Opcionalmente filtrado por estado (ACTIVO, INACTIVO).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    @GetMapping
    public ResponseEntity<List<TipoCuotaResponse>> listarTiposCuota(
            @RequestParam(required = false) EstadoTipoCuota estado) {
        log.info("GET /api/admin/tipos-cuota - estado={}", estado);
        return ResponseEntity.ok(tipoCuotaService.listarTiposCuota(estado));
    }

    @Operation(summary = "Ver el detalle de un tipo de cuota")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo de cuota encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un tipo de cuota con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<TipoCuotaResponse> obtenerTipoCuota(@PathVariable String id) {
        log.info("GET /api/admin/tipos-cuota/{}", id);
        return ResponseEntity.ok(tipoCuotaService.obtenerTipoCuotaPorId(id));
    }

    @Operation(summary = "Cambiar el estado de un tipo de cuota",
            description = "Un tipo INACTIVO deja de tenerse en cuenta al generar cuotas nuevas, pero las cuotas "
                    + "ya generadas con ese tipo no se ven afectadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe un tipo de cuota con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping("/{id}/estado")
    public ResponseEntity<CambiarEstadoTipoCuotaResponse> cambiarEstadoTipoCuota(
            @PathVariable String id,
            @Valid @RequestBody CambiarEstadoTipoCuotaRequest request) {
        log.info("PATCH /api/admin/tipos-cuota/{}/estado - nuevoEstado={}", id, request.nuevoEstado());
        return ResponseEntity.ok(tipoCuotaService.cambiarEstadoTipoCuota(id, request));
    }
}
