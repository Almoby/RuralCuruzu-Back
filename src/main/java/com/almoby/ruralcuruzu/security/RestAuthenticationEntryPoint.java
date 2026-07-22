package com.almoby.ruralcuruzu.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.almoby.ruralcuruzu.exception.ApiErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * Se dispara cuando un request sin autenticar (o con token inválido/revocado)
 * intenta acceder a una ruta protegida. Spring Security resuelve esto en el
 * filtro, ANTES de llegar a los controllers, así que GlobalExceptionHandler
 * (@RestControllerAdvice) nunca lo ve: por eso hace falta este componente,
 * para que la respuesta 401 tenga el mismo formato JSON que el resto de la API
 * en lugar de la página de error HTML por defecto de Spring.
 */
@Slf4j
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        log.warn("Acceso no autenticado rechazado [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), authException.getMessage());

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Necesitás iniciar sesión para acceder a este recurso",
                request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
