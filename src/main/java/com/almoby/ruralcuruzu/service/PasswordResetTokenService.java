package com.almoby.ruralcuruzu.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.TokenUnSoloUso;
import com.almoby.ruralcuruzu.enums.TipoTokenUnSoloUso;
import com.almoby.ruralcuruzu.exception.TokenRecuperacionExpiradoException;
import com.almoby.ruralcuruzu.exception.TokenRecuperacionInvalidoException;

import lombok.extern.slf4j.Slf4j;

/**
 * Genera y valida los tokens de un solo uso para "olvidé mi contraseña".
 * Wrapper fino sobre {@link TokenUnSoloUsoService} (mecánica compartida de
 * hash/TTL/un-solo-uso): esta clase solo aporta lo específico del flujo de
 * contraseña: el tiempo de expiración configurado y sus propias excepciones
 * (TokenRecuperacionInvalidoException/Expirado), que el resto del código
 * (AuthServiceImpl, GlobalExceptionHandler) ya conoce.
 */
@Slf4j
@Service
public class PasswordResetTokenService {

    private final TokenUnSoloUsoService tokenUnSoloUsoService;
    private final long expiracionMinutos;

    public PasswordResetTokenService(
            TokenUnSoloUsoService tokenUnSoloUsoService,
            @Value("${app.password-reset.expiration-minutes:30}") long expiracionMinutos) {
        this.tokenUnSoloUsoService = tokenUnSoloUsoService;
        this.expiracionMinutos = expiracionMinutos;
    }

    /**
     * Genera un nuevo token, guarda su hash y devuelve el token EN TEXTO PLANO
     * (el único momento en que existe: no queda persistido en ningún lado).
     */
    public String generar(String usuarioId) {
        return tokenUnSoloUsoService.generar(TipoTokenUnSoloUso.RESET_PASSWORD, usuarioId, expiracionMinutos);
    }

    /**
     * Valida el token recibido (sin usar todavía) y devuelve el id del usuario
     * dueño del token si es válido.
     *
     * @throws TokenRecuperacionInvalidoException si no existe o ya fue usado
     * @throws TokenRecuperacionExpiradoException si existe, no fue usado, pero venció
     */
    public String validarYObtenerUsuarioId(String tokenPlano) {
        TokenUnSoloUso token = tokenUnSoloUsoService.buscarPorTokenPlano(tokenPlano)
                .filter(t -> t.getTipo() == TipoTokenUnSoloUso.RESET_PASSWORD)
                .orElseThrow(() -> {
                    log.warn("Token de recuperación no encontrado (hash no coincide con ninguno)");
                    return new TokenRecuperacionInvalidoException();
                });

        if (token.isUsado()) {
            log.warn("Token de recuperación ya usado, usuarioId={}", token.getOwnerId());
            throw new TokenRecuperacionInvalidoException();
        }

        if (token.getExpiraEn().isBefore(Instant.now())) {
            log.warn("Token de recuperación expirado, usuarioId={} expiraba={}", token.getOwnerId(), token.getExpiraEn());
            throw new TokenRecuperacionExpiradoException();
        }

        return token.getOwnerId();
    }

    /**
     * Marca el token como usado para que no pueda volver a canjearse.
     * Se llama recién después de que la nueva contraseña se guardó con éxito.
     */
    public void marcarComoUsado(String tokenPlano) {
        tokenUnSoloUsoService.marcarComoUsado(tokenPlano);
    }
}
