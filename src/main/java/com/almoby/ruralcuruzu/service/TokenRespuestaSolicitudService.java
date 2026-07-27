package com.almoby.ruralcuruzu.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.TokenUnSoloUso;
import com.almoby.ruralcuruzu.enums.TipoTokenUnSoloUso;
import com.almoby.ruralcuruzu.exception.TokenRespuestaSolicitudExpiradoException;
import com.almoby.ruralcuruzu.exception.TokenRespuestaSolicitudInvalidoException;

import lombok.extern.slf4j.Slf4j;

/**
 * Genera y valida los tokens de un solo uso que le permiten a un
 * solicitante (sin cuenta todavía) responder una observación de su
 * solicitud. Wrapper fino sobre {@link TokenUnSoloUsoService} (mecánica
 * compartida de hash/TTL/un-solo-uso, la misma que usa
 * {@link PasswordResetTokenService} para "olvidé mi contraseña"): esta
 * clase solo aporta lo específico de este flujo: el tiempo de expiración
 * configurado y sus propias excepciones
 * (TokenRespuestaSolicitudInvalidoException/Expirado).
 */
@Slf4j
@Service
public class TokenRespuestaSolicitudService {

    private final TokenUnSoloUsoService tokenUnSoloUsoService;
    private final long expiracionMinutos;

    public TokenRespuestaSolicitudService(
            TokenUnSoloUsoService tokenUnSoloUsoService,
            @Value("${app.respuesta-solicitud.expiration-minutes:4320}") long expiracionMinutos) {
        this.tokenUnSoloUsoService = tokenUnSoloUsoService;
        this.expiracionMinutos = expiracionMinutos;
    }

    /**
     * Genera un nuevo token, guarda su hash y devuelve el token EN TEXTO
     * PLANO (el único momento en que existe: no queda persistido en ningún
     * lado). Invalida cualquier token anterior sin usar de esa solicitud.
     */
    public String generar(String numeroSolicitud) {
        return tokenUnSoloUsoService.generar(TipoTokenUnSoloUso.RESPUESTA_SOLICITUD, numeroSolicitud, expiracionMinutos);
    }

    /**
     * Valida el token recibido (sin usar todavía) y devuelve el número de
     * solicitud al que corresponde.
     *
     * @throws TokenRespuestaSolicitudInvalidoException si no existe o ya fue usado
     * @throws TokenRespuestaSolicitudExpiradoException si existe, no fue usado, pero venció
     */
    public String validarYObtenerNumeroSolicitud(String tokenPlano) {
        TokenUnSoloUso token = tokenUnSoloUsoService.buscarPorTokenPlano(tokenPlano)
                .filter(t -> t.getTipo() == TipoTokenUnSoloUso.RESPUESTA_SOLICITUD)
                .orElseThrow(() -> {
                    log.warn("Token de respuesta no encontrado (hash no coincide con ninguno)");
                    return new TokenRespuestaSolicitudInvalidoException();
                });

        if (token.isUsado()) {
            log.warn("Token de respuesta ya usado, numeroSolicitud={}", token.getOwnerId());
            throw new TokenRespuestaSolicitudInvalidoException();
        }

        if (token.getExpiraEn().isBefore(Instant.now())) {
            log.warn("Token de respuesta expirado, numeroSolicitud={} expiraba={}", token.getOwnerId(), token.getExpiraEn());
            throw new TokenRespuestaSolicitudExpiradoException();
        }

        return token.getOwnerId();
    }

    /** Marca el token como usado para que no pueda volver a canjearse. */
    public void marcarComoUsado(String tokenPlano) {
        tokenUnSoloUsoService.marcarComoUsado(tokenPlano);
    }
}
