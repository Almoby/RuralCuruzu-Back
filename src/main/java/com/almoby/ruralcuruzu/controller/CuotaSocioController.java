package com.almoby.ruralcuruzu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.request.InformarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.response.CuotaResumenResponse;
import com.almoby.ruralcuruzu.dto.response.InformarPagoResponse;
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

    public CuotaSocioController(CuotaService cuotaService) {
        this.cuotaService = cuotaService;
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
            description = "Autoservicio: el socio adjunta comprobante y datos de un pago que ya hizo (ej. por "
                    + "transferencia). La cuota pasa a EN_REVISION hasta que un admin la apruebe o rechace. Solo "
                    + "aplica a cuotas propias en estado PENDIENTE o VENCIDA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago informado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, o la cuota no admite informar un pago en su estado actual",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe esa cuota (o no es propia)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{cuotaId}/informar-pago")
    public ResponseEntity<InformarPagoResponse> informarPago(
            @PathVariable String cuotaId,
            @Valid @RequestBody InformarPagoCuotaRequest request,
            @AuthenticationPrincipal AuthenticatedUser socio) {
        String socioId = socio.usuario().getRefId();
        log.info("POST /api/socio/cuotas/{}/informar-pago - socioId={}", cuotaId, socioId);
        return ResponseEntity.ok(cuotaService.informarPago(cuotaId, request, socioId));
    }
}
