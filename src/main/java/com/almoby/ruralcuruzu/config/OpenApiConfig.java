package com.almoby.ruralcuruzu.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Configuración global de la documentación Swagger/OpenAPI.
 * Con esto ya generado, la doc interactiva queda disponible en:
 * - http://localhost:8080/swagger-ui.html (interfaz visual, para probar los endpoints a mano)
 * - http://localhost:8080/v3/api-docs (el JSON crudo de la spec OpenAPI, por si el
 *   frontend quiere generar un cliente HTTP automáticamente a partir de esto)
 *
 * El esquema de seguridad "bearerAuth" es lo que hace que Swagger UI muestre el
 * botón "Authorize" arriba a la derecha: ahí se pega el JWT (sin la palabra
 * "Bearer", Swagger la agrega sola) y así se pueden probar los endpoints
 * protegidos directamente desde el navegador, sin usar Postman.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Rural Curuzú - API",
                version = "v1",
                description = "API de la plataforma de beneficios de Rural Curuzú. "
                        + "Incluye autenticación (login, logout, recuperación de contraseña, "
                        + "renovación de sesión) para los tres tipos de cuenta: Socio, Comercio y Admin.",
                contact = @Contact(name = "Rural Curuzú")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Pegá acá el access token que te devuelve /api/auth/login (sin la palabra 'Bearer')."
)
public class OpenApiConfig {
}
