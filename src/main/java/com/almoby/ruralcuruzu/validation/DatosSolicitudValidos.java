package com.almoby.ruralcuruzu.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida que la solicitud traiga el sub-bloque de datos correcto según
 * tipoPersona: datosPersonaFisica cuando es FISICA, datosPersonaJuridica
 * cuando es JURIDICA, y no el otro (documento 5.2).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DatosSolicitudValidosValidator.class)
public @interface DatosSolicitudValidos {

    String message() default "Los datos de la solicitud no corresponden al tipo de persona indicado";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
