package com.almoby.ruralcuruzu.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida formato de teléfono: entre 8 y 15 dígitos, admite prefijo "+",
 * espacios, guiones y paréntesis como separadores (ej. "+54 9 3777 123456").
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TelefonoValidator.class)
public @interface Telefono {

    String message() default "El teléfono no tiene un formato válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
