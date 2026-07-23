package com.almoby.ruralcuruzu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.enums.EstadoSolicitud;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoSolicitudRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoSolicitudResponse;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioResponse;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioResumenResponse;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.security.AuthenticatedUser;
import com.almoby.ruralcuruzu.service.SolicitudSocioService;

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
 * Panel de administración de solicitudes de socio: listar, ver detalle y
 * cambiar de estado. Todo bajo /api/admin/**, restringido a ROLE_ADMIN en
 * SecurityConfig (no alcanza con estar logueado como Socio o Comercio).
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.ADMIN_SOLICITUDES_SOCIO_BASE)
@Tag(name = "Solicitud de Socio (Admin)", description = "Revisión y aprobación de solicitudes de socio. Solo ADMIN.")
@SecurityRequirement(name = "bearerAuth")
public class SolicitudSocioAdminController {

    private final SolicitudSocioService solicitudSocioService;

    public SolicitudSocioAdminController(SolicitudSocioService solicitudSocioService) {
        this.solicitudSocioService = solicitudSocioService;
    }

    @Operation(summary = "Listar solicitudes de socio",
            description = "Listado completo (sin paginación), opcionalmente filtrado por estado (PENDIENTE, "
                    + "EN_REVISION, APROBADA, RECHAZADA, CANCELADA). Sin filtro, devuelve todas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<SolicitudSocioResumenResponse>> listarSolicitud(
            @RequestParam(required = false) EstadoSolicitud estadoSolicitud) {
        log.info("GET /api/admin/solicitudes-socio - estado={}", estadoSolicitud);
        return ResponseEntity.ok(solicitudSocioService.listarSolicitudesSocio(estadoSolicitud));
    }

    @Operation(summary = "Ver el detalle de una solicitud",
            description = "Incluye los datos completos del solicitante y el historial de cambios de estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud encontrada"),
            @ApiResponse(responseCode = "404", description = "No existe una solicitud con ese número",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{numeroSolicitud}")
    public ResponseEntity<SolicitudSocioResponse> obtenerSolicitud(@PathVariable String numeroSolicitud) {
        log.info("GET /api/admin/solicitudes-socio/{} ", numeroSolicitud);
        return ResponseEntity.ok(solicitudSocioService.obtenerSolicitudSocioPorNumero(numeroSolicitud));
    }

    @Operation(summary = "Cambiar el estado de una solicitud",
            description = "Transiciones permitidas: PENDIENTE -> EN_REVISION/RECHAZADA/CANCELADA, "
                    + "EN_REVISION -> APROBADA/RECHAZADA/CANCELADA, y APROBADA/RECHAZADA/CANCELADA -> EN_REVISION "
                    + "('reabrir' la solicitud). El motivo es obligatorio al rechazar o cancelar. Al rechazar, "
                    + "además se le manda un correo al solicitante con el motivo. Todo queda registrado en el "
                    + "historial junto con el admin responsable, fecha y hora.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Transición de estado inválida, o falta el motivo "
                    + "obligatorio para rechazar/cancelar",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe una solicitud con ese número",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping("/{numeroSolicitud}/estado")
    public ResponseEntity<CambiarEstadoSolicitudResponse> cambiarEstadoSolicitud(
            @PathVariable String numeroSolicitud,
            @Valid @RequestBody CambiarEstadoSolicitudRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        log.info("PATCH /api/admin/solicitudes-socio/{}/estado - nuevoEstado={} admin={}",
                numeroSolicitud, request.nuevoEstado(), admin.usuario().getEmail());

        CambiarEstadoSolicitudResponse response = solicitudSocioService.cambiarEstadoSolicitudSocio(
                numeroSolicitud, request, admin.usuario().getId(), admin.usuario().getNombre());

        log.info("PATCH /api/admin/solicitudes-socio/{}/estado - actualizado a {}", numeroSolicitud, request.nuevoEstado());
        return ResponseEntity.ok(response);
    }
}
