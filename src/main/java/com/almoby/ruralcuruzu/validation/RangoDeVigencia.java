package com.almoby.ruralcuruzu.validation;

import java.time.LocalDate;

/**
 * Implementada por los requests de alta/edición de Beneficio, para poder
 * validar con una única regla (ver {@link RangoDeVigenciaValido}) que
 * fechaFinVigencia no sea anterior a fechaInicioVigencia, sin duplicar la
 * lógica en cada DTO.
 */
public interface RangoDeVigencia {

    LocalDate fechaInicioVigencia();

    LocalDate fechaFinVigencia();
}
