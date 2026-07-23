package com.almoby.ruralcuruzu.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida formato de D.N.I. argentino: 7 u 8 dígitos, con o sin puntos de miles
 * (ej. "28345678" o "28.345.678"). No valida existencia real de la persona,
 * solo formato.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DniValidator.class)
public @interface Dni {

    String message() default "El DNI no tiene un formato válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
