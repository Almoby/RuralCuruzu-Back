package com.almoby.ruralcuruzu.enums;

/**
 * Tipo de beneficio/promoción (documento, sección 14). El dueño del proyecto
 * aclaró que el comercio puede cargar "cualquier tipo" (2x1, 3x2, descuento en
 * porcentaje, etc.); OTRO queda como comodín para lo que no encaje en las
 * categorías más comunes, así el front igual puede mostrar un ícono/badge
 * razonable para la mayoría de los casos.
 */
public enum TipoBeneficio {
    DESCUENTO_PORCENTAJE,
    DOS_POR_UNO,
    TRES_POR_DOS,
    GRATIS,
    OTRO
}
