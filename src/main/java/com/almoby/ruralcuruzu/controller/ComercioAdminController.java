package com.almoby.ruralcuruzu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.request.ActualizarComercioParcialRequest;
import com.almoby.ruralcuruzu.dto.request.AltaComercioRequest;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoComercioRequest;
import com.almoby.ruralcuruzu.dto.request.EliminarComercioRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoComercioResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioActualizadoResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioEliminadoResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioResumenResponse;
import com.almoby.ruralcuruzu.dto.response.EliminarComercioResponse;
import com.almoby.ruralcuruzu.enums.EstadoComercio;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.security.AuthenticatedUser;
import com.almoby.ruralcuruzu.service.ComercioService;

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
 * Panel de administración de comercios adheridos (documento, sección 12).
 * Todo bajo /api/admin/comercios/**, restringido a ROLE_ADMIN (SecurityConfig).
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.ADMIN_COMERCIOS_BASE)
@Tag(name = "Comercios (Admin)", description = "Alta, listado, detalle y cambio de estado de comercios adheridos. Solo ADMIN.")
@SecurityRequirement(name = "bearerAuth")
public class ComercioAdminController {

    private final ComercioService comercioService;

    public ComercioAdminController(ComercioService comercioService) {
        this.comercioService = comercioService;
    }

    @Operation(summary = "Dar de alta un comercio",
            description = "Crea el comercio y, siempre, su Usuario con contraseña temporal y rol COMERCIO. Las "
                    + "credenciales se mandan por correo, y se exige cambiar la contraseña en el primer ingreso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comercio creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, o ya existe un comercio con ese "
                    + "CUIT o ese correo",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ComercioCreadoResponse> crearComercio(
            @Valid @RequestBody AltaComercioRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        log.info("POST /api/admin/comercios - cuit={} admin={}", request.cuit(), admin.usuario().getEmail());

        ComercioCreadoResponse response = comercioService.crearComercio(
                request, admin.usuario().getId(), admin.usuario().getNombre());

        log.info("POST /api/admin/comercios - comercio creado id={}", response.comercio().id());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar comercios",
            description = "Listado completo (sin paginación), opcionalmente filtrado por estado (ACTIVO, "
                    + "INACTIVO, SUSPENDIDO, DADO_DE_BAJA). Sin filtro, devuelve todos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    @GetMapping
    public ResponseEntity<List<ComercioResumenResponse>> listarComercios(
            @RequestParam(required = false) EstadoComercio estado) {
        log.info("GET /api/admin/comercios - estado={}", estado);
        return ResponseEntity.ok(comercioService.listarComercios(estado));
    }

    @Operation(summary = "Ver el historial de comercios eliminados",
            description = "Auditoría de bajas físicas (ver DELETE /{id}): comercios que ya no existen como tales, "
                    + "más recientes primero.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial obtenido correctamente")
    })
    @GetMapping("/eliminados")
    public ResponseEntity<List<ComercioEliminadoResponse>> listarComerciosEliminados() {
        log.info("GET /api/admin/comercios/eliminados");
        return ResponseEntity.ok(comercioService.listarComerciosEliminados());
    }

    @Operation(summary = "Ver el detalle de un comercio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comercio encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un comercio con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ComercioResponse> obtenerComercio(@PathVariable String id) {
        log.info("GET /api/admin/comercios/{}", id);
        return ResponseEntity.ok(comercioService.obtenerComercioPorId(id));
    }

    @Operation(summary = "Cambiar el estado de un comercio",
            description = "Sin restricciones de transición: se puede pasar libremente entre ACTIVO, INACTIVO, "
                    + "SUSPENDIDO y DADO_DE_BAJA. Mientras no esté ACTIVO, el comercio no puede iniciar sesión, "
                    + "no puede validar códigos QR, y sus promociones no se muestran (su historial se conserva "
                    + "siempre).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe un comercio con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping("/{id}/estado")
    public ResponseEntity<CambiarEstadoComercioResponse> cambiarEstadoComercio(
            @PathVariable String id,
            @Valid @RequestBody CambiarEstadoComercioRequest request) {
        log.info("PATCH /api/admin/comercios/{}/estado - nuevoEstado={}", id, request.nuevoEstado());

        CambiarEstadoComercioResponse response = comercioService.cambiarEstadoComercio(id, request);

        log.info("PATCH /api/admin/comercios/{}/estado - actualizado a {}", id, request.nuevoEstado());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Editar un comercio campo por campo",
            description = "Todos los campos son opcionales: solo se actualizan los que vengan con un valor no "
                    + "vacío en el body, el resto queda exactamente como estaba (ej. mandar solo `telefono` cambia "
                    + "únicamente el teléfono). Si cambia el correo electrónico, también se sincroniza el email de "
                    + "login de la cuenta de acceso del comercio, para que no queden desincronizados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comercio actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Algún campo enviado tiene un formato inválido, o el "
                    + "CUIT/correo ya lo usa otro comercio",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un comercio con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ComercioActualizadoResponse> actualizarComercioParcial(
            @PathVariable String id,
            @Valid @RequestBody ActualizarComercioParcialRequest request) {
        log.info("PATCH /api/admin/comercios/{}", id);

        ComercioActualizadoResponse response = comercioService.actualizarComercioParcial(id, request);

        log.info("PATCH /api/admin/comercios/{} - actualizado parcialmente", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar un comercio",
            description = "Borrado FÍSICO real (a diferencia de PATCH /{id}/estado con DADO_DE_BAJA, que es un "
                    + "estado reversible): el comercio deja de existir. Antes de borrar nada queda un registro de "
                    + "auditoría (ver GET /eliminados) con sus datos, el motivo y quién lo eliminó. En cascada, "
                    + "también se borran sus promociones y su cuenta de acceso (ya no puede loguearse); el "
                    + "historial de usos de sus promociones se conserva (tiene todos sus datos propios, no depende "
                    + "del comercio ni de las promociones originales).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comercio eliminado correctamente"),
            @ApiResponse(responseCode = "400", description = "El motivo es obligatorio",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un comercio con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<EliminarComercioResponse> eliminarComercio(
            @PathVariable String id,
            @Valid @RequestBody EliminarComercioRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        log.info("DELETE /api/admin/comercios/{} - admin={}", id, admin.usuario().getEmail());

        EliminarComercioResponse response = comercioService.eliminarComercio(
                id, request, admin.usuario().getId(), admin.usuario().getNombre());

        log.info("DELETE /api/admin/comercios/{} - eliminado", id);
        return ResponseEntity.ok(response);
    }
}
