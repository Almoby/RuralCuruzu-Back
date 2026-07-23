package com.almoby.ruralcuruzu.enums;

import com.almoby.ruralcuruzu.domain.DatosPersonaFisica;
import com.almoby.ruralcuruzu.domain.DatosPersonaJuridica;

/**
 * Indica si el solicitante es una persona física o una persona jurídica.
 * Determina qué subconjunto de datos ({@link DatosPersonaFisica} o
 * {@link DatosPersonaJuridica}) es obligatorio en la solicitud.
 */
public enum TipoPersona {
    FISICA,
    JURIDICA
}
