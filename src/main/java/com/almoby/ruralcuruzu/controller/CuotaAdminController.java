package com.almoby.ruralcuruzu.controller;

import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import com.almoby.ruralcuruzu.domain.Comprobante;
import com.almoby.ruralcuruzu.dto.request.AnularCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RegistrarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RevisarPagoInformadoRequest;
import com.almoby.ruralcuruzu.dto.response.CuotaResponse;
import com.almoby.ruralcuruzu.dto.response.CuotaResumenResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoCuentaSocioResponse;
import com.almoby.ruralcuruzu.dto.response.GeneracionCuotasResponse;
import com.almoby.ruralcuruzu.dto.response.PagoResponse;
import com.almoby.ruralcuruzu.dto.response.RegistrarPagoResponse;
import com.almoby.ruralcuruzu.dto.response.ResumenCuotasResponse;
import com.almoby.ruralcuruzu.dto.response.RevisarPagoInformadoResponse;
import com.almoby.ruralcuruzu.enums.EstadoCuota;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.exception.ArchivoInvalidoException;
import com.almoby.ruralcuruzu.security.AuthenticatedUser;
import com.almoby.ruralcuruzu.service.AlmacenamientoComprobantesService;
import com.almoby.ruralcuruzu.service.ComprobanteService;
import com.almoby.ruralcuruzu.service.CuotaService;
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
    private final ComprobanteService comprobanteService;
    private final AlmacenamientoComprobantesService almacenamientoComprobantesService;

    public CuotaAdminController(CuotaService cuotaService,
                                 ComprobanteService comprobanteService,
                                 AlmacenamientoComprobantesService almacenamientoComprobantesService) {
        this.cuotaService = cuotaService;
        this.comprobanteService = comprobanteService;
        this.almacenamientoComprobantesService = almacenamientoComprobantesService;
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

    @Operation(summary = "Ver el historial de generación de cuotas",
            description = "Documento 10.2, paso 8 (\"registrar la ejecución\"): cada corrida de generación, tanto "
                    + "automática (cron mensual, el 1º de cada mes) como manual, más recientes primero. Sirve para "
                    + "confirmar que el cron mensual efectivamente corrió.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial obtenido correctamente")
    })
    @GetMapping("/ejecuciones")
    public ResponseEntity<List<GeneracionCuotasResponse>> listarEjecuciones() {
        log.info("GET /api/admin/cuotas/ejecuciones");
        return ResponseEntity.ok(cuotaService.listarEjecuciones());
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

    @Operation(summary = "Ver el resumen de cuotas",
            description = "Totales para las tarjetas y pestañas del panel: total cobrado, total en revisión, "
                    + "total cobrado en efectivo, cantidades por estado (todas, pendientes -incluye vencidas y "
                    + "en revisión-, aprobadas, rechazadas), y el desglose de cobranzaPorCategoria (una fila por "
                    + "cada categoría de socio -ACTIVO/ADHERENTE-, aunque esté en cero) para el gráfico de Reportes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen calculado correctamente")
    })
    @GetMapping("/resumen")
    public ResponseEntity<ResumenCuotasResponse> obtenerResumen() {
        log.info("GET /api/admin/cuotas/resumen");
        return ResponseEntity.ok(cuotaService.obtenerResumen());
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
            description = "Documento 10.4 (ajustado al Figma): se elige un socio y uno o varios períodos "
                    + "(multi-select de meses, ej. pagar agosto + septiembre + octubre juntos), cada uno con una "
                    + "cuota ya generada. Todas quedan PAGADA con los mismos datos de pago (fecha, medio, "
                    + "comprobante, observación); el importe NO se pide en el body, se toma el de cada cuota "
                    + "(fijado al generarla), para que el admin no pueda registrar un monto que no coincida con lo "
                    + "adeudado. Se recalcula la deuda del socio (al consultar su estado de cuenta) y se le manda "
                    + "un correo de confirmación.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, o alguna cuota no admite un pago en su estado actual",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No hay una cuota generada para el socio en alguno de los períodos indicados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/pagos")
    public ResponseEntity<RegistrarPagoResponse> registrarPago(
            @Valid @RequestBody RegistrarPagoCuotaRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        log.info("POST /api/admin/cuotas/pagos - socioId={} periodos={} admin={}",
                request.socioId(), request.periodos(), admin.usuario().getEmail());
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
    public ResponseEntity<RevisarPagoInformadoResponse> revisarPagoInformado(
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

    @Operation(summary = "Descargar el comprobante de un pago",
            description = "Análogo a GET /api/socio/cuotas/pagos/{pagoId}/comprobante, pero sin la restricción de "
                    + "que sea un pago propio: el admin puede descargar el comprobante de cualquier pago (real, si "
                    + "el socio adjuntó uno al informar una transferencia; o una constancia en PDF, generada la "
                    + "primera vez que hace falta, si el pago ya está APROBADO y no tiene ninguno).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comprobante (real o generado) encontrado"),
            @ApiResponse(responseCode = "400", description = "Ese pago no tiene comprobante ni admite generar uno todavía (no está aprobado)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un pago con ese id",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/pagos/{pagoId}/comprobante")
    public ResponseEntity<Resource> descargarComprobante(
            @PathVariable String pagoId,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        log.info("GET /api/admin/cuotas/pagos/{}/comprobante - admin={}", pagoId, admin.usuario().getEmail());

        PagoResponse pago = cuotaService.obtenerPagoPorId(pagoId);

        Comprobante comprobante = comprobanteService.obtenerOGenerarParaPago(pago, pago.socioId())
                .orElseThrow(() -> new ArchivoInvalidoException("Ese pago no tiene comprobante adjunto"));

        Path archivo = almacenamientoComprobantesService.resolverParaDescarga(comprobante.getRuta());
        Resource recurso = new FileSystemResource(archivo);
        MediaType contentType = comprobante.getContentType() != null
                ? MediaType.parseMediaType(comprobante.getContentType())
                : ArchivoDescargaUtil.tipoDeContenido(archivo);
        String nombreDescarga = comprobante.getNombreArchivo() != null
                ? comprobante.getNombreArchivo()
                : ArchivoDescargaUtil.nombreParaDescarga(archivo);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreDescarga + "\"")
                .contentType(contentType)
                .body(recurso);
    }
}
