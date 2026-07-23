package com.almoby.ruralcuruzu.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.almoby.ruralcuruzu.constantes.ClavesRateLimiter;
import com.almoby.ruralcuruzu.dto.request.ForgotPasswordRequest;
import com.almoby.ruralcuruzu.dto.request.LoginRequest;
import com.almoby.ruralcuruzu.dto.request.LogoutRequest;
import com.almoby.ruralcuruzu.dto.request.RefreshTokenRequest;
import com.almoby.ruralcuruzu.dto.request.ResetPasswordRequest;
import com.almoby.ruralcuruzu.dto.response.LoginResponse;
import com.almoby.ruralcuruzu.dto.response.MensajeResponse;
import com.almoby.ruralcuruzu.exception.ApiErrorResponse;
import com.almoby.ruralcuruzu.exception.DemasiadasSolicitudesException;
import com.almoby.ruralcuruzu.security.RateLimiterService;
import com.almoby.ruralcuruzu.security.jwt.JwtAuthenticationFilter;
import com.almoby.ruralcuruzu.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@Tag(name = "Autenticación", description = "Login, logout, renovación de sesión y recuperación de contraseña "
        + "para Socios, Comercios y Admins.")
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final int maxIntentosLoginPorIp;

    public AuthController(AuthService authService,
                           RateLimiterService rateLimiterService,
                           @Value("${app.login.max-intentos-por-ip:20}") int maxIntentosLoginPorIp) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
        this.maxIntentosLoginPorIp = maxIntentosLoginPorIp;
    }

    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica a un Socio, Comercio o Admin con email y contraseña, y devuelve un "
                    + "access token (JWT, dura poco) más un refresh token (dura días, sirve para renovar sin "
                    + "volver a loguearse). El campo `rol` de la respuesta le indica al frontend a qué portal "
                    + "redirigir. Después de 5 contraseñas incorrectas seguidas, la cuenta queda bloqueada "
                    + "temporalmente; y no importa cuántas cuentas distintas se prueben, hay un límite de "
                    + "intentos por IP en poco tiempo para frenar ataques de fuerza bruta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso, devuelve el token y los datos del usuario"),
            @ApiResponse(responseCode = "401", description = "Email o contraseña incorrectos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "La cuenta existe pero está inactiva/suspendida",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "423", description = "Cuenta bloqueada temporalmente por intentos fallidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Demasiados intentos de login desde esta IP",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("POST /api/auth/login - intento de login para email={}", request.email());

        String ip = obtenerIpCliente(httpRequest);
        boolean permitido = rateLimiterService.permitirIntento(
                ClavesRateLimiter.PREFIJO_LOGIN_IP + ip, maxIntentosLoginPorIp, Duration.ofMinutes(5));

        if (!permitido) {
            log.warn("POST /api/auth/login - demasiados intentos desde ip={}", ip);
            throw new DemasiadasSolicitudesException();
        }

        LoginResponse response = authService.login(request);

        log.info("POST /api/auth/login - login exitoso para email={} rol={}", request.email(), response.rol());
        return ResponseEntity.ok(response);
    }

    /**
     * No usa getRemoteAddr() a secas porque, detrás de un proxy/balanceador
     * (lo habitual en producción), esa IP sería la del proxy y no la del
     * cliente real. X-Forwarded-For la trae si existe; si no, se usa
     * getRemoteAddr() como respaldo (típico en desarrollo local).
     */
    private String obtenerIpCliente(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Requiere un Bearer token válido (ruta protegida). El token en sí ya fue
     * validado y guardado como atributo del request por JwtAuthenticationFilter;
     * acá simplemente lo revocamos para que no pueda volver a usarse.
     */
    @Operation(
            summary = "Cerrar sesión",
            description = "Revoca el access token actual (queda en una lista negra, ya no sirve para "
                    + "autenticar aunque su firma y expiración sigan siendo técnicamente válidas). "
                    + "Si en el body se manda también el `refreshToken`, se revoca igual, cerrando la "
                    + "sesión por completo. Requiere estar logueado (header Authorization con el access token).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión cerrada correctamente"),
            @ApiResponse(responseCode = "401", description = "No hay token, o el token es inválido/ya expirado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<MensajeResponse> logout(
            @RequestAttribute(JwtAuthenticationFilter.ATRIBUTO_TOKEN_ACTUAL) String token,
            @RequestBody(required = false) LogoutRequest request) {
        log.info("POST /api/auth/logout - solicitud de logout");

        String refreshToken = request != null ? request.refreshToken() : null;
        authService.logout(token, refreshToken);

        log.info("POST /api/auth/logout - logout procesado correctamente");
        return ResponseEntity.ok(MensajeResponse.of("Sesión cerrada correctamente"));
    }

    /**
     * Ruta pública: la autorización acá la da poseer un refresh token válido,
     * no un Bearer access token (que puede ya haber expirado, es justamente
     * el caso de uso de este endpoint).
     */
    @Operation(
            summary = "Renovar el access token",
            description = "A partir de un refresh token válido, devuelve un access token nuevo (y también un "
                    + "refresh token nuevo: cada uno sirve una sola vez, se van 'rotando'). Sirve para que el "
                    + "frontend mantenga la sesión activa sin pedirle la contraseña de nuevo cada una hora. "
                    + "Ruta pública: no necesita el header Authorization, el propio refreshToken es la credencial.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Renovación exitosa, devuelve el nuevo access token"),
            @ApiResponse(responseCode = "401", description = "El refresh token no existe, ya fue usado, "
                    + "o el usuario ya no está activo",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("POST /api/auth/refresh - solicitud de renovación de token");

        LoginResponse response = authService.refrescarToken(request.refreshToken());

        log.info("POST /api/auth/refresh - token renovado para rol={}", response.rol());
        return ResponseEntity.ok(response);
    }

    /**
     * Ruta pública. La respuesta es siempre la misma exista o no el email,
     * para no permitir que alguien confirme qué emails están registrados.
     */
    @Operation(
            summary = "Solicitar recuperación de contraseña",
            description = "Si el email pertenece a una cuenta registrada, genera un enlace de un solo uso "
                    + "(vence a los 30 minutos) y lo manda por correo. La respuesta es SIEMPRE la misma, exista "
                    + "o no el email: así nadie puede usar este endpoint para averiguar qué emails están "
                    + "registrados en el sistema. Limitado a 3 solicitudes por email por hora.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud procesada (mensaje genérico, no confirma "
                    + "si el email existe)")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<MensajeResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("POST /api/auth/forgot-password - solicitud para email={}", request.email());

        authService.solicitarRecuperacionPassword(request.email());

        log.info("POST /api/auth/forgot-password - solicitud procesada para email={}", request.email());
        return ResponseEntity.ok(MensajeResponse.of(
                "Si el email existe en el sistema, vas a recibir un correo con las instrucciones para restablecer tu contraseña"));
    }

    /**
     * Ruta pública: la autorización acá la da poseer el token del email, no una sesión.
     */
    @Operation(
            summary = "Completar la recuperación de contraseña",
            description = "Recibe el token que llegó por correo (el que va en el link) y la nueva contraseña. "
                    + "Si todo es válido, actualiza la contraseña, invalida el token (no se puede reusar) y "
                    + "manda un correo de confirmación. La nueva contraseña no puede ser igual a la anterior.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Token inválido, ya usado, expirado, o la nueva "
                    + "contraseña es igual a la anterior",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<MensajeResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("POST /api/auth/reset-password - intento de restablecimiento de contraseña");

        authService.restablecerPassword(request.token(), request.nuevaPassword());

        log.info("POST /api/auth/reset-password - contraseña restablecida correctamente");
        return ResponseEntity.ok(MensajeResponse.of("Contraseña actualizada correctamente"));
    }
}
