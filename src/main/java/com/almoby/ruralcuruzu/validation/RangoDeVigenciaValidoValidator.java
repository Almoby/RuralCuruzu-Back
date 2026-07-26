package com.almoby.ruralcuruzu.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RangoDeVigenciaValidoValidator implements ConstraintValidator<RangoDeVigenciaValido, RangoDeVigencia> {

    @Override
    public boolean isValid(RangoDeVigencia request, ConstraintValidatorContext context) {
        if (request == null || request.fechaInicioVigencia() == null || request.fechaFinVigencia() == null) {
            // Ambos campos son opcionales; si falta alguno, no hay rango que comparar.
            return true;
        }
        if (!request.fechaFinVigencia().isBefore(request.fechaInicioVigencia())) {
            return true;
        }

        // Se asocia el error al campo fechaFinVigencia (en vez de dejarlo como
        // error "global" del objeto) para que el GlobalExceptionHandler, que
        // solo lee getFieldErrors(), lo incluya en la respuesta.
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("fechaFinVigencia")
                .addConstraintViolation();
        return false;
    }
}
