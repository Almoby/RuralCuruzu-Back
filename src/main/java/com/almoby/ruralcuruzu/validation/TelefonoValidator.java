package com.almoby.ruralcuruzu.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TelefonoValidator implements ConstraintValidator<Telefono, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        if (valor == null || valor.isBlank()) {
            return true;
        }
        if (!valor.matches("[+\\d][\\d\\s()-]*")) {
            return false;
        }
        String soloDigitos = valor.replaceAll("\\D", "");
        return soloDigitos.length() >= 8 && soloDigitos.length() <= 15;
    }
}
