package com.almoby.ruralcuruzu.constantes;

/**
 * Prefijos usados como clave en RateLimiterService. Cada uno identifica un
 * "balde" distinto (por IP, por email, etc.) para que dos límites distintos
 * no se pisen entre sí aunque compartan el mismo valor (ej. la misma IP
 * intentando loguearse y, aparte, enviando una solicitud de socio).
 */
public final class ClavesRateLimiter {

    public static final String PREFIJO_LOGIN_IP = "login-ip:";
    public static final String PREFIJO_FORGOT_PASSWORD = "forgot-password:";
    public static final String PREFIJO_SOLICITUD_SOCIO_IP = "solicitud-socio-ip:";

    private ClavesRateLimiter() {
    }
}
