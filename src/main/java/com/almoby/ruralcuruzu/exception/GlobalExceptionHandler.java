package com.almoby.ruralcuruzu.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.almoby.ruralcuruzu.exception.ApiErrorResponse.CampoError;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Traduce cada excepción de negocio a una respuesta HTTP consistente.
 * Ningún controller debería construir un ResponseEntity de error a mano:
 * lanzan la excepción correspondiente y este handler decide el código y el formato.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiErrorResponse> handleCredencialesInvalidas(CredencialesInvalidasException ex,
                                                                         HttpServletRequest request) {
        log.warn("Login rechazado [{}]: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                                  HttpServletRequest request) {
        log.warn("Login rechazado [{}]: credenciales inválidas (Spring Security)", request.getRequestURI());
        return responder(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos", request);
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<ApiErrorResponse> handleUsuarioInactivo(UsuarioInactivoException ex,
                                                                   HttpServletRequest request) {
        log.warn("Login rechazado [{}]: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(CuentaBloqueadaTemporalmenteException.class)
    public ResponseEntity<ApiErrorResponse> handleCuentaBloqueada(CuentaBloqueadaTemporalmenteException ex,
                                                                    HttpServletRequest request) {
        log.warn("Login rechazado [{}]: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.LOCKED, ex.getMessage(), request);
    }

    @ExceptionHandler(DemasiadasSolicitudesException.class)
    public ResponseEntity<ApiErrorResponse> handleDemasiadasSolicitudes(DemasiadasSolicitudesException ex,
                                                                          HttpServletRequest request) {
        log.warn("Rate limit excedido [{}]: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
    }

    @ExceptionHandler(TokenRecuperacionInvalidoException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenRecuperacionInvalido(TokenRecuperacionInvalidoException ex,
                                                                             HttpServletRequest request) {
        log.warn("Recuperación de contraseña rechazada [{}]: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(TokenRecuperacionExpiradoException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenRecuperacionExpirado(TokenRecuperacionExpiradoException ex,
                                                                             HttpServletRequest request) {
        log.warn("Recuperación de contraseña rechazada [{}]: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(PasswordIgualException.class)
    public ResponseEntity<ApiErrorResponse> handlePasswordIgual(PasswordIgualException ex,
                                                                  HttpServletRequest request) {
        log.warn("Restablecimiento de contraseña rechazado [{}]: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(RefreshTokenInvalidoException.class)
    public ResponseEntity<ApiErrorResponse> handleRefreshTokenInvalido(RefreshTokenInvalidoException ex,
                                                                         HttpServletRequest request) {
        log.warn("Refresh rechazado [{}]: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(RefreshTokenExpiradoException.class)
    public ResponseEntity<ApiErrorResponse> handleRefreshTokenExpirado(RefreshTokenExpiradoException ex,
                                                                         HttpServletRequest request) {
        log.warn("Refresh rechazado [{}]: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                                HttpServletRequest request) {
        log.warn("Acceso denegado [{}]: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.FORBIDDEN, "No tenés permisos para acceder a este recurso", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidacion(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        List<CampoError> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new CampoError(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        log.warn("Validación fallida [{}]: {}", request.getRequestURI(), errores);

        ApiErrorResponse body = ApiErrorResponse.ofValidacion(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Hay datos inválidos en la petición",
                request.getRequestURI(),
                errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleJsonMalformado(HttpMessageNotReadableException ex,
                                                                   HttpServletRequest request) {
        log.warn("Cuerpo de la petición inválido o ausente [{}]: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición falta o no es un JSON válido", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleParametroFaltante(MissingServletRequestParameterException ex,
                                                                      HttpServletRequest request) {
        log.warn("Parámetro obligatorio ausente [{}]: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: " + ex.getParameterName(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMetodoNoSoportado(HttpRequestMethodNotSupportedException ex,
                                                                      HttpServletRequest request) {
        log.warn("Método HTTP no soportado [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.METHOD_NOT_ALLOWED,
                "El método " + request.getMethod() + " no está permitido para esta ruta", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRutaNoEncontrada(NoResourceFoundException ex,
                                                                     HttpServletRequest request) {
        log.warn("Ruta no encontrada [{} {}]", request.getMethod(), request.getRequestURI());
        return responder(HttpStatus.NOT_FOUND, "El recurso solicitado no existe", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenerico(Exception ex, HttpServletRequest request) {
        // Este es el único caso donde logueamos el stack trace completo:
        // si llegamos acá es porque no anticipamos este error, así que necesitamos verlo entero.
        log.error("Error inesperado en [{}]", request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", request);
    }

    private ResponseEntity<ApiErrorResponse> responder(HttpStatus status, String mensaje,
                                                        HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                status.value(), status.getReasonPhrase(), mensaje, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
