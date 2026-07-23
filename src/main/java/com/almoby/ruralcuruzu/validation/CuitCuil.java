package com.almoby.ruralcuruzu.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida formato y dígito verificador de CUIT/CUIL argentino: 11 dígitos
 * (ej. "20-28345678-9" o "20283456789"), con el dígito verificador calculado
 * según el algoritmo oficial de AFIP (módulo 11).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CuitCuilValidator.class)
public @interface CuitCuil {

    String message() default "El CUIT/CUIL no tiene un formato válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
