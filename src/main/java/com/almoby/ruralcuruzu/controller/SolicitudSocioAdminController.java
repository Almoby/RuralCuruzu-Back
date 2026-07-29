package com.almoby.ruralcuruzu.controller;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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
import com.almoby.ruralcuruzu.enums.EstadoSolicitud;
import com.almoby.ruralcuruzu.dto.request.AgregarObservacionSolicitudRequest;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoSolicitudRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoSolicitudResponse;
import com.almoby.ruralcuruzu.dto.response.ObservacionAgregadaResponse;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioResponse;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioResumenResponse;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.exception.ArchivoInvalidoException;
import com.almoby.ruralcuruzu.security.AuthenticatedUser;
import com.almoby.ruralcuruzu.service.AlmacenamientoArchivosService;
import com.almoby.ruralcuruzu.service.SolicitudSocioService;
import com.almoby.ruralcuruzu.util.ArchivoDescargaUtil;

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
    private final AlmacenamientoArchivosService almacenamientoArchivosService;

    public SolicitudSocioAdminController(SolicitudSocioService solicitudSocioService,
                                          AlmacenamientoArchivosService almacenamientoArchivosService) {
        this.solicitudSocioService = solicitudSocioService;
        this.almacenamientoArchivosService = almacenamientoArchivosService;
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
            description = "Transiciones permitidas: PENDIENTE -> EN_REVISION (toda solicitud nueva debe pasar "
                    + "primero por revisión, no se aprueba/rechaza/cancela directo), EN_REVISION -> "
                    + "APROBADA/RECHAZADA/CANCELADA, y RECHAZADA -> EN_REVISION ('reabrir' la solicitud). "
                    + "APROBADA y CANCELADA son estados finales: no admiten ninguna transición. El motivo es "
                    + "obligatorio al rechazar o cancelar. Al aprobar, se crea el Socio y su Usuario con "
                    + "credenciales temporales (documento, sección 8.4). Al rechazar, además se le manda un "
                    + "correo al solicitante con el motivo. Todo queda registrado en el historial junto con el "
                    + "admin responsable, fecha y hora.")
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

    @Operation(summary = "Agregar una observación a una solicitud",
            description = "Deja una nota en el historial sin cambiar el estado de la solicitud, y le manda un "
                    + "correo al solicitante con el texto. Sirve tanto para 'solicitar correcciones' como "
                    + "'solicitar documentación' (documento, sección 8.3): son casos de uso de una misma "
                    + "observación visible para el solicitante.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Observación agregada correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe una solicitud con ese número",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{numeroSolicitud}/observaciones")
    public ResponseEntity<ObservacionAgregadaResponse> agregarObservacion(
            @PathVariable String numeroSolicitud,
            @Valid @RequestBody AgregarObservacionSolicitudRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        log.info("POST /api/admin/solicitudes-socio/{}/observaciones - admin={}", numeroSolicitud, admin.usuario().getEmail());

        ObservacionAgregadaResponse response = solicitudSocioService.agregarObservacion(
                numeroSolicitud, request.observacion(), admin.usuario().getId(), admin.usuario().getNombre());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Descargar un archivo adjunto de una solicitud",
            description = "Descarga documentación que el solicitante adjuntó al responder una observación (link "
                    + "del correo, ver POST /api/respuesta-solicitud). `ruta` tiene que ser exactamente uno de "
                    + "los valores que aparecen en `archivosAdjuntos` del historial de esta misma solicitud "
                    + "(GET /{numeroSolicitud}).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archivo encontrado"),
            @ApiResponse(responseCode = "400", description = "La ruta indicada no corresponde a un archivo "
                    + "adjunto de esta solicitud, o el archivo ya no existe en disco",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe una solicitud con ese número",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{numeroSolicitud}/archivos")
    public ResponseEntity<Resource> descargarArchivo(
            @PathVariable String numeroSolicitud,
            @RequestParam String ruta,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        log.info("GET /api/admin/solicitudes-socio/{}/archivos - admin={} ruta={}",
                numeroSolicitud, admin.usuario().getEmail(), ruta);

        // No alcanza con que el token del admin sea válido: además verificamos que
        // la ruta pedida realmente pertenezca al historial de ESTA solicitud, para
        // que nadie pueda descargar documentación de otra solicitud adivinando rutas.
        SolicitudSocioResponse solicitud = solicitudSocioService.obtenerSolicitudSocioPorNumero(numeroSolicitud);
        boolean rutaPerteneceALaSolicitud = solicitud.historial().stream()
                .flatMap(cambio -> cambio.archivosAdjuntos() == null ? Stream.empty() : cambio.archivosAdjuntos().stream())
                .anyMatch(ruta::equals);
        if (!rutaPerteneceALaSolicitud) {
            log.warn("Descarga rechazada: ruta={} no pertenece al historial de numeroSolicitud={}", ruta, numeroSolicitud);
            throw new ArchivoInvalidoException("El archivo solicitado no pertenece a esta solicitud");
        }

        Path archivo = almacenamientoArchivosService.resolverParaDescarga(ruta);
        Resource recurso = new FileSystemResource(archivo);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + ArchivoDescargaUtil.nombreParaDescarga(archivo) + "\"")
                .contentType(ArchivoDescargaUtil.tipoDeContenido(archivo))
                .body(recurso);
    }
}
