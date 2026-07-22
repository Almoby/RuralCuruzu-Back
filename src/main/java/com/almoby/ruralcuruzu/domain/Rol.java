package com.almoby.ruralcuruzu.domain;

/**
 * Roles de usuario soportados por la plataforma.
 * Cada rol determina a qué portal se redirige tras el login
 * y qué endpoints puede utilizar.
 */
public enum Rol {
    SOCIO,
    COMERCIO,
    ADMIN
}
