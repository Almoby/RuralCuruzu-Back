package com.almoby.ruralcuruzu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.response.SocioResponse;
import com.almoby.ruralcuruzu.dto.response.SocioResumenResponse;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.service.SocioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Consulta de socios ya dados de alta (documento, sección 8.4). Todo bajo
 * /api/admin/socios/**, restringido a ROLE_ADMIN (SecurityConfig). El alta en
 * sí no ocurre acá: se dispara al aprobar una solicitud (SolicitudSocioService).
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.ADMIN_SOCIOS_BASE)
@Tag(name = "Socios (Admin)", description = "Listado y detalle de socios. Solo ADMIN.")
@SecurityRequirement(name = "bearerAuth")
public class SocioAdminController {

    private final SocioService socioService;

    public SocioAdminController(SocioService socioService) {
        this.socioService = socioService;
    }

    @Operation(summary = "Listar socios",
            description = "Listado completo (sin paginación), opcionalmente filtrado por estado (ACTIVO, "
                    + "INACTIVO, DADO_DE_BAJA). Sin filtro, devuelve todos. Usado, entre otras cosas, para poblar "
                    + "el select de socio del modal \"Registrar pago\" de Cuotas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    @GetMapping
    public ResponseEntity<List<SocioResumenResponse>> listarSocios(
            @RequestParam(required = false) EstadoSocio estado) {
        log.info("GET /api/admin/socios - estado={}", estado);
        return ResponseEntity.ok(socioService.listarSocios(estado));
    }

    @Operation(summary = "Ver el detalle de un socio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Socio encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un socio con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<SocioResponse> obtenerSocio(@PathVariable String id) {
        log.info("GET /api/admin/socios/{}", id);
        return ResponseEntity.ok(socioService.obtenerSocioPorId(id));
    }
}
