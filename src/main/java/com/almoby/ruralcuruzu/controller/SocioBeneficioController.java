package com.almoby.ruralcuruzu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.response.BeneficioResumenResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioConBeneficiosResponse;
import com.almoby.ruralcuruzu.dto.response.HistorialBeneficioResponse;
import com.almoby.ruralcuruzu.security.AuthenticatedUser;
import com.almoby.ruralcuruzu.service.BeneficioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Consulta del socio sobre beneficios, comercios adheridos y su propio
 * historial de usos (documento, sección 14). Todo bajo /api/socio/beneficios/**,
 * restringido a ROLE_SOCIO (SecurityConfig).
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.SOCIO_BENEFICIOS_BASE)
@Tag(name = "Beneficios (Socio)", description = "Listado de beneficios/comercios y propio historial de usos. Solo SOCIO.")
@SecurityRequirement(name = "bearerAuth")
public class SocioBeneficioController {

    private final BeneficioService beneficioService;

    public SocioBeneficioController(BeneficioService beneficioService) {
        this.beneficioService = beneficioService;
    }

    @Operation(summary = "Listar beneficios vigentes (pestaña \"Promociones\")",
            description = "Solo beneficios ACTIVOS y dentro de su vigencia por fecha. Filtros opcionales por rubro "
                    + "(ej. Farmacia, Gastronomía) y por texto libre (busca en título y nombre del comercio).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    @GetMapping
    public ResponseEntity<List<BeneficioResumenResponse>> listarBeneficios(
            @RequestParam(required = false) String rubro,
            @RequestParam(required = false) String busqueda) {
        log.info("GET /api/socio/beneficios - rubro={} busqueda={}", rubro, busqueda);
        return ResponseEntity.ok(beneficioService.listarBeneficiosVigentes(rubro, busqueda));
    }

    @Operation(summary = "Listar comercios con beneficios vigentes (pestaña \"Comercios\")",
            description = "Mismos filtros que el listado de beneficios, pero agrupado por comercio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    @GetMapping("/comercios-con-beneficios")
    public ResponseEntity<List<ComercioConBeneficiosResponse>> listarComercios(
            @RequestParam(required = false) String rubro,
            @RequestParam(required = false) String busqueda) {
        log.info("GET /api/socio/beneficios/comercios-con-beneficios - rubro={} busqueda={}", rubro, busqueda);
        return ResponseEntity.ok(beneficioService.listarComerciosConBeneficios(rubro, busqueda));
    }

    @Operation(summary = "Ver mi historial de beneficios usados", description = "Documento, secciones 14.4 y 19.3. Más recientes primero.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial obtenido correctamente")
    })
    @GetMapping("/historial-beneficios")
    public ResponseEntity<List<HistorialBeneficioResponse>> miHistorial(@AuthenticationPrincipal AuthenticatedUser socio) {
        String socioId = socio.usuario().getRefId();
        log.info("GET /api/socio/beneficios/historial-beneficios - socioId={}", socioId);
        return ResponseEntity.ok(beneficioService.listarHistorialDeSocio(socioId));
    }
}
