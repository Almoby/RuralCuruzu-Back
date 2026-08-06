package com.almoby.ruralcuruzu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.response.TipoBeneficioResponse;
import com.almoby.ruralcuruzu.service.TipoBeneficioCatalogoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * A propósito sin prefijo /admin, /socio o /comercio (mismo criterio que
 * NotificacionController): lo consulta el comercio para armar el dropdown al
 * crear/editar un beneficio, y también puede servirle al admin, así que
 * cualquier rol autenticado puede pegarle (cubierto por el
 * ".anyRequest().authenticated()" de SecurityConfig).
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.TIPOS_BENEFICIO_BASE)
@Tag(name = "Tipos de Beneficio", description = "Catálogo de tipos de beneficio disponibles (solo los activos).")
@SecurityRequirement(name = "bearerAuth")
public class TipoBeneficioController {

    private final TipoBeneficioCatalogoService tipoBeneficioCatalogoService;

    public TipoBeneficioController(TipoBeneficioCatalogoService tipoBeneficioCatalogoService) {
        this.tipoBeneficioCatalogoService = tipoBeneficioCatalogoService;
    }

    @Operation(summary = "Listar los tipos de beneficio activos", description = "Para poblar el dropdown al crear o editar un beneficio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    @GetMapping
    public ResponseEntity<List<TipoBeneficioResponse>> listarActivos() {
        log.info("GET /api/tipos-beneficio");
        return ResponseEntity.ok(tipoBeneficioCatalogoService.listarActivos());
    }
}
