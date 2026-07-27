package com.almoby.ruralcuruzu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.request.ActualizarBeneficioRequest;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoBeneficioRequest;
import com.almoby.ruralcuruzu.dto.request.CrearBeneficioRequest;
import com.almoby.ruralcuruzu.dto.request.ValidarBeneficioRequest;
import com.almoby.ruralcuruzu.dto.response.BeneficioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.BeneficioResponse;
import com.almoby.ruralcuruzu.dto.response.ValidarBeneficioResponse;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.security.AuthenticatedUser;
import com.almoby.ruralcuruzu.service.BeneficioService;

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
 * Autoservicio del comercio sobre sus propios beneficios (documento, sección
 * 14). Todo bajo /api/comercio/beneficios/**, restringido a ROLE_COMERCIO
 * (SecurityConfig). El comercioId nunca viaja en la URL ni en el body:
 * siempre se resuelve desde el usuario autenticado (usuario.refId), así un
 * comercio no puede ver ni tocar beneficios ajenos.
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.COMERCIO_BENEFICIOS_BASE)
@Tag(name = "Beneficios (Comercio)", description = "Alta, edición y validación de beneficios propios. Solo COMERCIO.")
@SecurityRequirement(name = "bearerAuth")
public class ComercioBeneficioController {

    private final BeneficioService beneficioService;

    public ComercioBeneficioController(BeneficioService beneficioService) {
        this.beneficioService = beneficioService;
    }

    @Operation(summary = "Crear un beneficio propio",
            description = "Cualquier tipo (descuento en porcentaje, 2x1, 3x2, gratis, etc.). Queda ACTIVO desde que se crea.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Beneficio creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<BeneficioCreadoResponse> crearBeneficio(
            @Valid @RequestBody CrearBeneficioRequest request,
            @AuthenticationPrincipal AuthenticatedUser comercio) {
        String comercioId = comercio.usuario().getRefId();
        log.info("POST /api/comercio/beneficios - comercioId={} titulo={}", comercioId, request.titulo());
        return ResponseEntity.ok(beneficioService.crearBeneficio(comercioId, request));
    }

    @Operation(summary = "Listar mis beneficios")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    @GetMapping
    public ResponseEntity<List<BeneficioResponse>> listarMisBeneficios(@AuthenticationPrincipal AuthenticatedUser comercio) {
        String comercioId = comercio.usuario().getRefId();
        log.info("GET /api/comercio/beneficios - comercioId={}", comercioId);
        return ResponseEntity.ok(beneficioService.listarBeneficiosDelComercio(comercioId));
    }

    @Operation(summary = "Ver el detalle de un beneficio propio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Beneficio encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe (o no es propio)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<BeneficioResponse> obtenerBeneficio(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser comercio) {
        String comercioId = comercio.usuario().getRefId();
        log.info("GET /api/comercio/beneficios/{} - comercioId={}", id, comercioId);
        return ResponseEntity.ok(beneficioService.obtenerBeneficioDelComercio(comercioId, id));
    }

    @Operation(summary = "Editar un beneficio propio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Beneficio actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe (o no es propio)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<BeneficioResponse> actualizarBeneficio(
            @PathVariable String id,
            @Valid @RequestBody ActualizarBeneficioRequest request,
            @AuthenticationPrincipal AuthenticatedUser comercio) {
        String comercioId = comercio.usuario().getRefId();
        log.info("PUT /api/comercio/beneficios/{} - comercioId={}", id, comercioId);
        return ResponseEntity.ok(beneficioService.actualizarBeneficio(comercioId, id, request));
    }

    @Operation(summary = "Activar o pausar un beneficio propio",
            description = "INACTIVO deja de mostrarse a los socios, sin borrar el historial de usos ya registrados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe (o no es propio)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping("/{id}/estado")
    public ResponseEntity<BeneficioResponse> cambiarEstado(
            @PathVariable String id,
            @Valid @RequestBody CambiarEstadoBeneficioRequest request,
            @AuthenticationPrincipal AuthenticatedUser comercio) {
        String comercioId = comercio.usuario().getRefId();
        log.info("PATCH /api/comercio/beneficios/{}/estado - comercioId={} nuevoEstado={}", id, comercioId, request.nuevoEstado());
        return ResponseEntity.ok(beneficioService.cambiarEstadoBeneficio(comercioId, id, request));
    }

    @Operation(summary = "Escanear el QR de un socio y aplicar un beneficio",
            description = "Flujo de uso (documento, sección 14): el socio muestra su QR, el comercio lo escanea y "
                    + "elige cuál de sus propios beneficios aplicar, junto con el ahorro real de esa compra. Si el "
                    + "QR es válido y el beneficio está vigente, queda un registro para el socio y el comercio. "
                    + "Cada beneficio se puede canjear una única vez por socio (para siempre, no por día).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Beneficio aplicado correctamente"),
            @ApiResponse(responseCode = "400", description = "El token del QR expiró (pedirle al socio que lo "
                    + "actualice), el beneficio no está vigente (pausado o vencido), o el QR del socio no está "
                    + "activo (dado de baja, con cuotas vencidas, o cuenta suspendida/bloqueada)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "El token del QR no corresponde a ningún socio (o "
                    + "está manipulado), o el beneficio no existe (o no es propio)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ese socio ya había canjeado este beneficio antes",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/canjear-beneficio")
    public ResponseEntity<ValidarBeneficioResponse> canjearBeneficio(
            @Valid @RequestBody ValidarBeneficioRequest request,
            @AuthenticationPrincipal AuthenticatedUser comercio) {
        String comercioId = comercio.usuario().getRefId();
        log.info("POST /api/comercio/beneficios/canjear-beneficio - comercioId={} beneficioId={}", comercioId, request.beneficioId());
        return ResponseEntity.ok(beneficioService.validarYUsarBeneficio(comercioId, comercio.usuario().getId(), request));
    }
}
