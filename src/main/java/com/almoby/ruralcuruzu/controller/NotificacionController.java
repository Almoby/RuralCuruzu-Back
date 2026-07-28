package com.almoby.ruralcuruzu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.response.ContadorNoLeidasResponse;
import com.almoby.ruralcuruzu.dto.response.NotificacionResponse;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.security.AuthenticatedUser;
import com.almoby.ruralcuruzu.service.NotificacionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * La campanita (documento, sección 29). A propósito NO está separado en
 * /admin, /socio y /comercio como el resto de los módulos: cualquier rol
 * autenticado ve y administra únicamente sus propias notificaciones (se
 * resuelven siempre desde el usuario autenticado, nunca desde la URL o el
 * body), así que un único controller alcanza sin duplicar la misma lógica
 * tres veces.
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.NOTIFICACIONES_BASE)
@Tag(name = "Notificaciones", description = "Campanita in-app: cualquier rol ve únicamente las suyas.")
@SecurityRequirement(name = "bearerAuth")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @Operation(summary = "Ver mis notificaciones", description = "Más recientes primero, leídas y no leídas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> misNotificaciones(
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        log.info("GET /api/notificaciones - usuarioId={}", usuario.usuario().getId());
        return ResponseEntity.ok(notificacionService.listarPropias(usuario.usuario().getId()));
    }

    @Operation(summary = "Contar mis notificaciones no leídas", description = "Para el badge numérico de la campanita.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contador obtenido correctamente")
    })
    @GetMapping("/no-leidas/contador")
    public ResponseEntity<ContadorNoLeidasResponse> contarNoLeidas(@AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(notificacionService.contarNoLeidas(usuario.usuario().getId()));
    }

    @Operation(summary = "Marcar una notificación propia como leída")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marcada como leída correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe esa notificación, o no es propia",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable String id, @AuthenticationPrincipal AuthenticatedUser usuario) {
        log.info("PATCH /api/notificaciones/{}/leida - usuarioId={}", id, usuario.usuario().getId());
        notificacionService.marcarLeida(id, usuario.usuario().getId());
        return ResponseEntity.ok().build();
    }
}
