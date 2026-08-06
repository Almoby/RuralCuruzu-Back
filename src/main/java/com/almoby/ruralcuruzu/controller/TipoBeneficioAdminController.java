package com.almoby.ruralcuruzu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.request.ActualizarTipoBeneficioRequest;
import com.almoby.ruralcuruzu.dto.request.CrearTipoBeneficioRequest;
import com.almoby.ruralcuruzu.dto.response.MensajeResponse;
import com.almoby.ruralcuruzu.dto.response.TipoBeneficioActualizadoResponse;
import com.almoby.ruralcuruzu.dto.response.TipoBeneficioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.TipoBeneficioResponse;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.service.TipoBeneficioCatalogoService;

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
 * Administración del catálogo de tipos de beneficio (mejora futura pedida
 * por el front, reemplaza al enum TipoBeneficio fijo). Todo bajo
 * /api/admin/tipos-beneficio/**, restringido a ROLE_ADMIN (SecurityConfig).
 * El listado de tipos activos para el dropdown del comercio vive aparte, en
 * {@link TipoBeneficioController} (GET /api/tipos-beneficio).
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.ADMIN_TIPOS_BENEFICIO_BASE)
@Tag(name = "Tipos de Beneficio (Admin)", description = "Catálogo administrable de tipos de beneficio/promoción. Solo ADMIN.")
@SecurityRequirement(name = "bearerAuth")
public class TipoBeneficioAdminController {

    private final TipoBeneficioCatalogoService tipoBeneficioCatalogoService;

    public TipoBeneficioAdminController(TipoBeneficioCatalogoService tipoBeneficioCatalogoService) {
        this.tipoBeneficioCatalogoService = tipoBeneficioCatalogoService;
    }

    @Operation(summary = "Listar todos los tipos de beneficio", description = "Incluye activos e inactivos (a diferencia de GET /api/tipos-beneficio, que solo trae los activos).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    @GetMapping
    public ResponseEntity<List<TipoBeneficioResponse>> listarTodos() {
        log.info("GET /api/admin/tipos-beneficio");
        return ResponseEntity.ok(tipoBeneficioCatalogoService.listarTodos());
    }

    @Operation(summary = "Ver el detalle de un tipo de beneficio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo de beneficio encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un tipo de beneficio con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<TipoBeneficioResponse> obtener(@PathVariable String id) {
        log.info("GET /api/admin/tipos-beneficio/{}", id);
        return ResponseEntity.ok(tipoBeneficioCatalogoService.obtenerPorId(id));
    }

    @Operation(summary = "Crear un tipo de beneficio", description = "Nace activo. El código debe ser único.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo de beneficio creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un tipo de beneficio con ese código",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TipoBeneficioCreadoResponse> crear(@Valid @RequestBody CrearTipoBeneficioRequest request) {
        log.info("POST /api/admin/tipos-beneficio - codigo={}", request.codigo());
        return ResponseEntity.ok(tipoBeneficioCatalogoService.crear(request));
    }

    @Operation(summary = "Editar un tipo de beneficio", description = "Edición parcial: nombre y/o activo. El código no se puede cambiar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo de beneficio actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe un tipo de beneficio con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<TipoBeneficioActualizadoResponse> actualizar(
            @PathVariable String id, @Valid @RequestBody ActualizarTipoBeneficioRequest request) {
        log.info("PATCH /api/admin/tipos-beneficio/{}", id);
        return ResponseEntity.ok(tipoBeneficioCatalogoService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar un tipo de beneficio",
            description = "Borrado físico. Si hay beneficios cargados con este tipo, se rechaza (desactivalo en cambio con PATCH activo=false).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo de beneficio eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe un tipo de beneficio con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Hay beneficios que usan este tipo",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<MensajeResponse> eliminar(@PathVariable String id) {
        log.info("DELETE /api/admin/tipos-beneficio/{}", id);
        tipoBeneficioCatalogoService.eliminar(id);
        return ResponseEntity.ok(MensajeResponse.of("Tipo de beneficio eliminado correctamente"));
    }
}
