package com.almoby.ruralcuruzu.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.dto.response.MensajeResponse;
import com.almoby.ruralcuruzu.dto.response.ObservacionPendienteResponse;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.service.SolicitudSocioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Ruta pública sin login a la que llega el solicitante desde el link del
 * correo de "observación" (documento, sección 8.3: "solicitar
 * correcciones"/"solicitar documentación"). El solicitante todavía no tiene
 * cuenta, así que en vez de sesión, cada acción se valida con el token de un
 * solo uso que viaja en la URL (mismo criterio que /api/auth/reset-password).
 */
@Slf4j
@RestController
@RequestMapping(RutasApi.RESPUESTA_SOLICITUD_BASE)
@Tag(name = "Respuesta a Solicitud", description = "Ruta pública para que el solicitante responda una observación (sin necesidad de cuenta).")
public class RespuestaSolicitudController {

    private final SolicitudSocioService solicitudSocioService;

    public RespuestaSolicitudController(SolicitudSocioService solicitudSocioService) {
        this.solicitudSocioService = solicitudSocioService;
    }

    @Operation(
            summary = "Consultar la observación pendiente de responder",
            description = "Valida el token del correo y devuelve la última observación dejada por un admin, "
                    + "para mostrarle al solicitante a qué está respondiendo antes de que envíe nada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Observación encontrada"),
            @ApiResponse(responseCode = "400", description = "El enlace no es válido, ya fue utilizado, o venció",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ObservacionPendienteResponse> consultarObservacionPendiente(@RequestParam String token) {
        log.info("GET /api/respuesta-solicitud - consultando observación pendiente");

        ObservacionPendienteResponse response = solicitudSocioService.consultarObservacionPendiente(token);

        log.info("GET /api/respuesta-solicitud - observación encontrada numeroSolicitud={}", response.numeroSolicitud());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Responder una observación",
            description = "Valida el token del correo (de un solo uso) y registra la respuesta del solicitante en "
                    + "el historial de la solicitud, con la documentación adjunta que haya subido (PDF, JPG o PNG). "
                    + "Avisa por correo a todos los admins para que puedan retomar la revisión.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Respuesta registrada correctamente"),
            @ApiResponse(responseCode = "400", description = "El enlace no es válido/ya fue usado/venció, "
                    + "o alguno de los archivos adjuntos no es de un tipo permitido",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "El archivo (o el conjunto de archivos) es demasiado grande",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MensajeResponse> responderObservacion(
            @RequestParam String token,
            @RequestParam String texto,
            @RequestParam(required = false) List<MultipartFile> archivos) {
        log.info("POST /api/respuesta-solicitud - registrando respuesta a observación");

        solicitudSocioService.responderObservacion(token, texto, archivos);

        log.info("POST /api/respuesta-solicitud - respuesta registrada correctamente");
        return ResponseEntity.ok(MensajeResponse.of("Respuesta enviada correctamente"));
    }
}
