package com.almoby.ruralcuruzu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.request.AnularCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RegistrarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RevisarPagoInformadoRequest;
import com.almoby.ruralcuruzu.dto.response.CuotaResponse;
import com.almoby.ruralcuruzu.dto.response.CuotaResumenResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoCuentaSocioResponse;
import com.almoby.ruralcuruzu.dto.response.GeneracionCuotasResponse;
import com.almoby.ruralcuruzu.dto.response.RegistrarPagoResponse;
import com.almoby.ruralcuruzu.enums.EstadoCuota;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.security.AuthenticatedUser;
import com.almoby.ruralcuruzu.service.CuotaService;

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
 * Administración de cuotas (documento, sección 10). Todo bajo
 * /api/admin/cuotas/**, restringido a ROLE_ADMIN (SecurityConfig).
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.ADMIN_CUOTAS_BASE)
@Tag(name = "Cuotas (Admin)", description = "Generación, listado, registro de pagos y revisión de cuotas. Solo ADMIN.")
@SecurityRequirement(name = "bearerAuth")
public class CuotaAdminController {

    private final CuotaService cuotaService;

    public CuotaAdminController(CuotaService cuotaService) {
        this.cuotaService = cuotaService;
    }

    @Operation(summary = "Generar cuotas manualmente",
            description = "Dispara la misma lógica que corre automáticamente el 1º de cada mes (documento 10.2), "
                    + "para el período indicado (formato yyyy-MM) o el mes actual si no se especifica. Omite a los "
                    + "socios que ya tengan una cuota generada para ese período, y a los que no tengan un tipo de "
                    + "cuota vigente para su categoría (quedan contados en cantidadSociosOmitidos).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Generación ejecutada correctamente")
    })
    @PostMapping("/generar")
    public ResponseEntity<GeneracionCuotasResponse> generarCuotas(
            @RequestParam(required = false) String periodo,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        log.info("POST /api/admin/cuotas/generar - periodo={} admin={}", periodo, admin.usuario().getEmail());

        GeneracionCuotasResponse response = cuotaService.generarCuotas(
                periodo, admin.usuario().getId(), admin.usuario().getNombre());

        log.info("POST /api/admin/cuotas/generar - generadas={} omitidos={}",
                response.cantidadCuotasGeneradas(), response.cantidadSociosOmitidos());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar cuotas", description = "Sin paginación, filtros opcionales combinables por estado, socio y período (yyyy-MM).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    @GetMapping
    public ResponseEntity<List<CuotaResumenResponse>> listarCuotas(
            @RequestParam(required = false) EstadoCuota estado,
            @RequestParam(required = false) String socioId,
            @RequestParam(required = false) String periodo) {
        log.info("GET /api/admin/cuotas - estado={} socioId={} periodo={}", estado, socioId, periodo);
        return ResponseEntity.ok(cuotaService.listarCuotas(estado, socioId, periodo));
    }

    @Operation(summary = "Ver el detalle de una cuota")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuota encontrada"),
            @ApiResponse(responseCode = "404", description = "No existe una cuota con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<CuotaResponse> obtenerCuota(@PathVariable String id) {
        log.info("GET /api/admin/cuotas/{}", id);
        return ResponseEntity.ok(cuotaService.obtenerCuotaPorId(id));
    }

    @Operation(summary = "Ver el estado de cuenta de un socio", description = "Deuda total y detalle de todas sus cuotas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de cuenta obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe un socio con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/estado-cuenta/{socioId}")
    public ResponseEntity<EstadoCuentaSocioResponse> obtenerEstadoCuenta(@PathVariable String socioId) {
        log.info("GET /api/admin/cuotas/estado-cuenta/{}", socioId);
        return ResponseEntity.ok(cuotaService.obtenerEstadoCuentaSocio(socioId));
    }

    @Operation(summary = "Registrar un pago manual",
            description = "Documento 10.4: puede cubrir una o varias cuotas a la vez. Todas quedan PAGADA con los "
                    + "mismos datos de pago (fecha, medio, comprobante, observación), se recalcula la deuda del "
                    + "socio (al consultar su estado de cuenta) y se le manda un correo de confirmación.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, o alguna cuota no admite un pago en su estado actual",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Alguna de las cuotas no existe",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/pagos")
    public ResponseEntity<RegistrarPagoResponse> registrarPago(
            @Valid @RequestBody RegistrarPagoCuotaRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        log.info("POST /api/admin/cuotas/pagos - cuotaIds={} admin={}", request.cuotaIds(), admin.usuario().getEmail());
        return ResponseEntity.ok(cuotaService.registrarPago(request, admin.usuario().getId(), admin.usuario().getNombre()));
    }

    @Operation(summary = "Aprobar o rechazar un pago informado por el socio",
            description = "Solo aplica a cuotas en estado EN_REVISION. Aprobar la pasa a PAGADA; rechazar requiere motivo y la pasa a RECHAZADA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Revisión resuelta correctamente"),
            @ApiResponse(responseCode = "400", description = "La cuota no está EN_REVISION, o falta el motivo de rechazo",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe una cuota con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping("/{id}/revision")
    public ResponseEntity<CuotaResponse> revisarPagoInformado(
            @PathVariable String id,
            @Valid @RequestBody RevisarPagoInformadoRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        log.info("PATCH /api/admin/cuotas/{}/revision - aprobar={} admin={}", id, request.aprobar(), admin.usuario().getEmail());
        return ResponseEntity.ok(cuotaService.revisarPagoInformado(
                id, request, admin.usuario().getId(), admin.usuario().getNombre()));
    }

    @Operation(summary = "Anular una cuota", description = "Ej: se generó por error. No cuenta como deuda del socio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuota anulada correctamente"),
            @ApiResponse(responseCode = "400", description = "La cuota ya está PAGADA o ANULADA",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe una cuota con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping("/{id}/anular")
    public ResponseEntity<CuotaResponse> anularCuota(
            @PathVariable String id,
            @Valid @RequestBody AnularCuotaRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        log.info("PATCH /api/admin/cuotas/{}/anular - admin={}", id, admin.usuario().getEmail());
        return ResponseEntity.ok(cuotaService.anularCuota(id, request, admin.usuario().getId(), admin.usuario().getNombre()));
    }
}
