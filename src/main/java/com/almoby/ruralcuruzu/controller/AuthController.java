package com.almoby.ruralcuruzu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.dto.request.ForgotPasswordRequest;
import com.almoby.ruralcuruzu.dto.request.LoginRequest;
import com.almoby.ruralcuruzu.dto.request.ResetPasswordRequest;
import com.almoby.ruralcuruzu.dto.response.LoginResponse;
import com.almoby.ruralcuruzu.security.jwt.JwtAuthenticationFilter;
import com.almoby.ruralcuruzu.service.AuthService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Único punto de entrada de autenticación para los tres portales
 * (Socio, Comercio, Admin). El rol devuelto en la respuesta es el que
 * usa el frontend para redirigir al portal correspondiente.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login - intento de login para email={}", request.email());

        LoginResponse response = authService.login(request);

        log.info("POST /api/auth/login - login exitoso para email={} rol={}", request.email(), response.rol());
        return ResponseEntity.ok(response);
    }

    /**
     * Requiere un Bearer token válido (ruta protegida). El token en sí ya fue
     * validado y guardado como atributo del request por JwtAuthenticationFilter;
     * acá simplemente lo revocamos para que no pueda volver a usarse.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestAttribute(JwtAuthenticationFilter.ATRIBUTO_TOKEN_ACTUAL) String token) {
        log.info("POST /api/auth/logout - solicitud de logout");

        authService.logout(token);

        log.info("POST /api/auth/logout - logout procesado correctamente");
        return ResponseEntity.noContent().build();
    }

    /**
     * Ruta pública. La respuesta es siempre la misma exista o no el email,
     * para no permitir que alguien confirme qué emails están registrados.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("POST /api/auth/forgot-password - solicitud para email={}", request.email());

        authService.solicitarRecuperacionPassword(request.email());

        log.info("POST /api/auth/forgot-password - solicitud procesada para email={}", request.email());
        return ResponseEntity.noContent().build();
    }

    /**
     * Ruta pública: la autorización acá la da poseer el token del email, no una sesión.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("POST /api/auth/reset-password - intento de restablecimiento de contraseña");

        authService.restablecerPassword(request.token(), request.nuevaPassword());

        log.info("POST /api/auth/reset-password - contraseña restablecida correctamente");
        return ResponseEntity.noContent().build();
    }
}
