package com.almoby.ruralcuruzu.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida que, si ambas fechas están presentes, fechaFinVigencia no sea
 * anterior a fechaInicioVigencia (aplica a alta y edición de Beneficio).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RangoDeVigenciaValidoValidator.class)
public @interface RangoDeVigenciaValido {

    String message() default "La fecha de fin de vigencia no puede ser anterior a la fecha de inicio";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
