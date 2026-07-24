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
    public static final String ADMIN_COMERCIOS_BASE = "/api/admin/comercios";
    public static final String ADMIN_COMERCIOS = ADMIN_COMERCIOS_BASE + "/**";
    public static final String ADMIN_TIPOS_CUOTA_BASE = "/api/admin/tipos-cuota";
    public static final String ADMIN_TIPOS_CUOTA = ADMIN_TIPOS_CUOTA_BASE + "/**";
    public static final String ADMIN_CUOTAS_BASE = "/api/admin/cuotas";
    public static final String ADMIN_CUOTAS = ADMIN_CUOTAS_BASE + "/**";
    public static final String SOCIO_CUOTAS_BASE = "/api/socio/cuotas";
    public static final String SOCIO_CUOTAS = SOCIO_CUOTAS_BASE + "/**";

    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
    public static final String SWAGGER_UI = "/swagger-ui/**";
    public static final String API_DOCS = "/v3/api-docs/**";

    private RutasApi() {
    }
}
