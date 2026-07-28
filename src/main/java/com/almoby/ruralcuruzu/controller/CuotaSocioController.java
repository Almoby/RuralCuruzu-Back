package com.almoby.ruralcuruzu.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.request.InformarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.response.CuotaResumenResponse;
import com.almoby.ruralcuruzu.dto.response.DatosBancariosResponse;
import com.almoby.ruralcuruzu.dto.response.InformarPagoResponse;
import com.almoby.ruralcuruzu.dto.response.LinkDePagoResponse;
import com.almoby.ruralcuruzu.dto.response.PagoResponse;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.exception.ArchivoInvalidoException;
import com.almoby.ruralcuruzu.security.AuthenticatedUser;
import com.almoby.ruralcuruzu.service.AlmacenamientoComprobantesService;
import com.almoby.ruralcuruzu.service.CuotaService;
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
 * Autoservicio del socio para sus propias cuotas (documento, sección 10).
 * Todo bajo /api/socio/cuotas/**, restringido a ROLE_SOCIO (SecurityConfig).
 * El socioId nunca viaja en la URL ni en el body: siempre se resuelve desde
 * el usuario autenticado (usuario.refId), así un socio no puede ver ni
 * informar pagos de cuotas ajenas.
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.SOCIO_CUOTAS_BASE)
@Tag(name = "Cuotas (Socio)", description = "Un socio ve sus propias cuotas e informa sus pagos. Solo SOCIO.")
@SecurityRequirement(name = "bearerAuth")
public class CuotaSocioController {

    private final CuotaService cuotaService;
    private final AlmacenamientoComprobantesService almacenamientoComprobantesService;
    private final DatosBancariosService datosBancariosService;

    public CuotaSocioController(CuotaService cuotaService,
                                 AlmacenamientoComprobantesService almacenamientoComprobantesService,
                                 DatosBancariosService datosBancariosService) {
        this.cuotaService = cuotaService;
        this.almacenamientoComprobantesService = almacenamientoComprobantesService;
        this.datosBancariosService = datosBancariosService;
    }

    @Operation(summary = "Ver mis cuotas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    @GetMapping
    public ResponseEntity<List<CuotaResumenResponse>> misCuotas(@AuthenticationPrincipal AuthenticatedUser socio) {
        String socioId = socio.usuario().getRefId();
        log.info("GET /api/socio/cuotas - socioId={}", socioId);
        return ResponseEntity.ok(cuotaService.listarCuotasDeSocio(socioId));
    }

    @Operation(summary = "Informar el pago de una cuota propia",
            description = "Autoservicio: el socio adjunta el comprobante (PDF, JPG o PNG, obligatorio) y los datos "
                    + "de una transferencia que ya hizo (único medio válido acá: los pagos en efectivo, débito o "
                    + "presenciales se pagan y registran directamente en la oficina, y el pago online tiene su "
                    + "propio endpoint, /link-de-pago). La cuota pasa a EN_REVISION hasta que un admin la apruebe "
                    + "o rechace. Aplica a cuotas propias en estado PENDIENTE, VENCIDA o RECHAZADA (un rechazo "
                    + "previo no impide volver a intentar: RN-17, cada intento queda como su propio registro).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago informado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, falta el comprobante, el "
                    + "comprobante no es de un tipo permitido, o la cuota no admite informar un pago en su "
                    + "estado actual",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe esa cuota (o no es propia)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping(value = "/{cuotaId}/informar-pago", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InformarPagoResponse> informarPago(
            @PathVariable String cuotaId,
            @Valid @RequestPart("datos") InformarPagoCuotaRequest request,
            @RequestPart("comprobante") MultipartFile comprobante,
            @AuthenticationPrincipal AuthenticatedUser socio) {
        String socioId = socio.usuario().getRefId();
        log.info("POST /api/socio/cuotas/{}/informar-pago - socioId={}", cuotaId, socioId);
        return ResponseEntity.ok(cuotaService.informarPago(cuotaId, request, comprobante, socioId));
    }

    @Operation(summary = "Generar un link de pago (Mercado Pago) para una cuota propia",
            description = "Autoservicio: crea una preferencia de pago en Mercado Pago y devuelve la URL de "
                    + "checkout a la que hay que redirigir al socio. A diferencia de informar un pago por "
                    + "transferencia, acá no hace falta comprobante ni revisión de un admin: la propia pasarela "
                    + "confirma el resultado vía webhook (asincrónico, puede tardar). Aplica a cuotas propias en "
                    + "estado PENDIENTE, VENCIDA o RECHAZADA (RN-17: un intento previo rechazado no impide otro).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Link de pago generado correctamente"),
            @ApiResponse(responseCode = "400", description = "La cuota no admite un nuevo intento de pago en su "
                    + "estado actual",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe esa cuota (o no es propia)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Mercado Pago no está configurado, o falló al crear "
                    + "la preferencia de pago",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{cuotaId}/link-de-pago")
    public ResponseEntity<LinkDePagoResponse> generarLinkDePago(
            @PathVariable String cuotaId,
            @AuthenticationPrincipal AuthenticatedUser socio) {
        String socioId = socio.usuario().getRefId();
        log.info("POST /api/socio/cuotas/{}/link-de-pago - socioId={}", cuotaId, socioId);
        return ResponseEntity.ok(cuotaService.generarLinkDePago(cuotaId, socioId));
    }

    @Operation(summary = "Ver mi historial de pagos",
            description = "RN-17: cada intento de pago es un registro propio e inmutable, más recientes primero. "
                    + "A diferencia de GET /api/socio/cuotas (una fila por cuota), acá aparecen TODOS los intentos "
                    + "de todas las cuotas, incluyendo los rechazados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial obtenido correctamente")
    })
    @GetMapping("/pagos")
    public ResponseEntity<List<PagoResponse>> misPagos(@AuthenticationPrincipal AuthenticatedUser socio) {
        String socioId = socio.usuario().getRefId();
        log.info("GET /api/socio/cuotas/pagos - socioId={}", socioId);
        return ResponseEntity.ok(cuotaService.listarPagosDeSocio(socioId));
    }

    @Operation(summary = "Descargar el comprobante de un pago propio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comprobante encontrado"),
            @ApiResponse(responseCode = "400", description = "Ese pago no tiene comprobante, o no es propio",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/pagos/{pagoId}/comprobante")
    public ResponseEntity<Resource> descargarComprobante(
            @PathVariable String pagoId,
            @AuthenticationPrincipal AuthenticatedUser socio) {
        String socioId = socio.usuario().getRefId();
        log.info("GET /api/socio/cuotas/pagos/{}/comprobante - socioId={}", pagoId, socioId);

        // No alcanza con que el socio esté logueado: además verificamos que el pago
        // pedido sea realmente suyo, para que nadie descargue el comprobante de otro
        // socio adivinando ids.
        String rutaComprobante = cuotaService.listarPagosDeSocio(socioId).stream()
                .filter(pago -> pago.id().equals(pagoId))
                .map(PagoResponse::comprobanteRuta)
                .findFirst()
                .orElseThrow(() -> new ArchivoInvalidoException("Ese pago no tiene comprobante, o no es propio"));

        if (rutaComprobante == null) {
            throw new ArchivoInvalidoException("Ese pago no tiene comprobante adjunto");
        }

        Path archivo = almacenamientoComprobantesService.resolverParaDescarga(rutaComprobante);
        Resource recurso = new FileSystemResource(archivo);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreParaDescarga(archivo) + "\"")
                .contentType(tipoDeContenido(archivo))
                .body(recurso);
    }

    @Operation(summary = "Ver los datos bancarios para transferir",
            description = "Cuenta de la cooperativa (banco, CBU, alias, titular, CUIT) para que el socio pague una "
                    + "cuota por transferencia. Son los mismos datos para todos los socios (no es por-socio).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos bancarios obtenidos correctamente"),
            @ApiResponse(responseCode = "404", description = "Todavía no fueron configurados por un admin",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/datos-bancarios")
    public ResponseEntity<DatosBancariosResponse> datosBancarios() {
        log.info("GET /api/socio/cuotas/datos-bancarios");
        return ResponseEntity.ok(datosBancariosService.obtener());
    }

    /** Le saca el prefijo UUID_ con el que se guardó el archivo, para que el socio vea el nombre original. */
    private String nombreParaDescarga(Path archivo) {
        return archivo.getFileName().toString().replaceFirst(
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_", "");
    }

    private MediaType tipoDeContenido(Path archivo) {
        try {
            String contentType = Files.probeContentType(archivo);
            return contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (IOException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
