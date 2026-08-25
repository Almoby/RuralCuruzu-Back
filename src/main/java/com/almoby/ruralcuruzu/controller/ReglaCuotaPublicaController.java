package com.almoby.ruralcuruzu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.response.ReglaCuotaResponse;
import com.almoby.ruralcuruzu.service.ReglaCuotaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Consulta pública (sin login) del valor de la cuota por categoría, para
 * mostrarlo en el formulario "Quiero ser socio" antes de que el visitante
 * mande la solicitud (pedido del dueño del proyecto). Solo lectura, mismos
 * datos que ya ve el admin en /api/admin/reglas-cuota, sin nada sensible.
 * Todo bajo /api/reglas-cuota/**, público (ver SecurityConfig).
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.REGLAS_CUOTA_PUBLICAS_BASE)
@Tag(name = "Reglas de Cuota (Público)", description = "Valor de la cuota por categoría, visible sin login.")
public class ReglaCuotaPublicaController {

    private final ReglaCuotaService reglaCuotaService;

    public ReglaCuotaPublicaController(ReglaCuotaService reglaCuotaService) {
        this.reglaCuotaService = reglaCuotaService;
    }

    @Operation(summary = "Ver el valor de la cuota por categoría",
            description = "Como máximo dos filas (ACTIVO y ADHERENTE). Si una categoría todavía no tiene regla "
                    + "cargada, no aparece en la lista.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    @GetMapping
    public ResponseEntity<List<ReglaCuotaResponse>> listarValoresDeCuota() {
        log.info("GET /api/reglas-cuota");
        return ResponseEntity.ok(reglaCuotaService.listarReglas());
    }
}
