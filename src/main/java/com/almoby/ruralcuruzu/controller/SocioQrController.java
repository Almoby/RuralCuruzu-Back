package com.almoby.ruralcuruzu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.response.MiQrResponse;
import com.almoby.ruralcuruzu.security.AuthenticatedUser;
import com.almoby.ruralcuruzu.service.SocioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Autoservicio del socio sobre su propio perfil. Todo bajo /api/socio/perfil/**,
 * restringido a ROLE_SOCIO (SecurityConfig).
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.SOCIO_PERFIL_BASE)
@Tag(name = "Perfil (Socio)", description = "Datos propios del socio autenticado. Solo SOCIO.")
@SecurityRequirement(name = "bearerAuth")
public class SocioQrController {

    private final SocioService socioService;

    public SocioQrController(SocioService socioService) {
        this.socioService = socioService;
    }

    @Operation(summary = "Ver mi código QR",
            description = "Código único del socio para el módulo de Beneficios: lo muestra en pantalla y el "
                    + "comercio lo escanea para aplicarle un beneficio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código obtenido correctamente")
    })
    @GetMapping("/mi-qr")
    public ResponseEntity<MiQrResponse> miQr(@AuthenticationPrincipal AuthenticatedUser socio) {
        String socioId = socio.usuario().getRefId();
        log.info("GET /api/socio/perfil/mi-qr - socioId={}", socioId);
        return ResponseEntity.ok(socioService.obtenerMiQr(socioId));
    }
}
