package com.almoby.ruralcuruzu.constantes;

/**
 * Rutas de la API usadas para configurar seguridad (SecurityConfig). Centralizadas
 * acá para no tener los mismos literales de ruta repetidos entre la config de
 * seguridad y los controllers que los exponen.
 */
public final class RutasApi {

    public static final String LOGIN = "/api/auth/login";
    public static final String FORGOT_PASSWORD = "/api/auth/forgot-password";
    public static final String RESET_PASSWORD = "/api/auth/reset-password";
    public static final String REFRESH = "/api/auth/refresh";
    public static final String SOLICITUDES_SOCIO = "/api/solicitudes-socio";
    public static final String ADMIN_SOLICITUDES_SOCIO_BASE = "/api/admin/solicitudes-socio";
    public static final String ADMIN_SOLICITUDES_SOCIO = ADMIN_SOLICITUDES_SOCIO_BASE + "/**";

    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
    public static final String SWAGGER_UI = "/swagger-ui/**";
    public static final String API_DOCS = "/v3/api-docs/**";

    private RutasApi() {
    }
}
