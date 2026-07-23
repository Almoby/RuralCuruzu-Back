package com.almoby.ruralcuruzu.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DniValidator implements ConstraintValidator<Dni, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        if (valor == null || valor.isBlank()) {
            // La obligatoriedad la controla @NotBlank en el campo, no este validador.
            return true;
        }
        String soloDigitos = valor.replaceAll("[.\\s]", "");
        return soloDigitos.matches("\\d{7,8}");
    }
}
