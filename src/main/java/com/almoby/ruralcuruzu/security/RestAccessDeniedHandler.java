package com.almoby.ruralcuruzu.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.almoby.ruralcuruzu.exception.ApiErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * Se dispara cuando un usuario SÍ está autenticado pero su rol no le permite
 * acceder a la ruta (ej. un Socio pidiendo un endpoint solo de Admin).
 * Igual que RestAuthenticationEntryPoint, esto ocurre a nivel filtro y nunca
 * llega a GlobalExceptionHandler, por eso necesita su propio manejo para
 * devolver JSON consistente en vez de la página de error por defecto.
 */
@Slf4j
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Acceso denegado [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "No tenés permisos para acceder a este recurso",
                request.getRequestURI());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
