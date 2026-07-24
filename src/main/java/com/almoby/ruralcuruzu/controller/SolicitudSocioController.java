package com.almoby.ruralcuruzu.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.ClavesRateLimiter;
import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.request.SolicitudSocioRequest;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioCreadaResponse;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.exception.DemasiadasSolicitudesException;
import com.almoby.ruralcuruzu.security.RateLimiterService;
import com.almoby.ruralcuruzu.service.SolicitudSocioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Endpoint público del botón "Quiero ser socio" (documento, sección 4).
 * No requiere estar logueado: cualquier visitante puede enviar una solicitud.
 * La revisión y aprobación las hace un admin desde SolicitudSocioAdminController.
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.SOLICITUDES_SOCIO)
@Tag(name = "Solicitud de Socio", description = "Alta pública de solicitudes para ser socio (persona física o jurídica).")
public class SolicitudSocioController {

    private final SolicitudSocioService solicitudSocioService;
    private final RateLimiterService rateLimiterService;
    private final int maxSolicitudesPorIp;

    public SolicitudSocioController(SolicitudSocioService solicitudSocioService,
                                     RateLimiterService rateLimiterService,
                                     @Value("${app.solicitud-socio.max-por-ip:10}") int maxSolicitudesPorIp) {
        this.solicitudSocioService = solicitudSocioService;
        this.rateLimiterService = rateLimiterService;
        this.maxSolicitudesPorIp = maxSolicitudesPorIp;
    }

    @Operation(
            summary = "Enviar una solicitud para ser socio",
            description = "Ruta pública (botón 'Quiero ser socio' en la pantalla inicial). Crea la solicitud en "
                    + "estado PENDIENTE, genera un número de solicitud y envía un correo de confirmación. "
                    + "Todavía NO crea ninguna cuenta de usuario: eso ocurre recién si un admin la aprueba. "
                    + "Exactamente uno de `datosPersonaFisica` / `datosPersonaJuridica` debe venir completo, "
                    + "según `tipoPersona`.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud creada, devuelve el número de solicitud"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, email/DNI/CUIT ya registrado, "
                    + "o no se aceptaron los términos y condiciones",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Demasiadas solicitudes desde esta IP",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<SolicitudSocioCreadaResponse> crearSolicitud(@Valid @RequestBody SolicitudSocioRequest request,
                                                          HttpServletRequest httpRequest) {
        log.info("POST /api/solicitudes-socio - nueva solicitud, tipoPersona={} categoria={}",
                request.tipoPersona(), request.categoriaSolicitada());

        String ip = obtenerIpCliente(httpRequest);
        boolean permitido = rateLimiterService.permitirIntento(
                ClavesRateLimiter.PREFIJO_SOLICITUD_SOCIO_IP + ip, maxSolicitudesPorIp, Duration.ofHours(1));
        if (!permitido) {
            log.warn("POST /api/solicitudes-socio - demasiadas solicitudes desde ip={}", ip);
            throw new DemasiadasSolicitudesException();
        }

        SolicitudSocioCreadaResponse response = solicitudSocioService.crearSolicitudSocio(request);

        log.info("POST /api/solicitudes-socio - solicitud creada numeroSolicitud={}", response.solicitud().numeroSolicitud());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
