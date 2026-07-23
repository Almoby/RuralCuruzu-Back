package com.almoby.ruralcuruzu.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Algoritmo oficial (AFIP) de dígito verificador para CUIT/CUIL:
 * se multiplica cada uno de los primeros 10 dígitos por la serie
 * 5,4,3,2,7,6,5,4,3,2, se suman los resultados, se calcula el resto
 * de dividir por 11 y el dígito verificador es 11 - resto (con los
 * casos especiales resto=11 -> 0 y resto=10 -> 9).
 */
public class CuitCuilValidator implements ConstraintValidator<CuitCuil, String> {

    private static final int[] MULTIPLICADORES = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        if (valor == null || valor.isBlank()) {
            return true;
        }

        String soloDigitos = valor.replaceAll("[-.\\s]", "");
        if (!soloDigitos.matches("\\d{11}")) {
            return false;
        }

        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += Character.getNumericValue(soloDigitos.charAt(i)) * MULTIPLICADORES[i];
        }

        int resto = suma % 11;
        int verificadorEsperado = switch (resto) {
            case 11 -> 0;
            case 10 -> 9;
            default -> 11 - resto;
        };

        int verificadorRecibido = Character.getNumericValue(soloDigitos.charAt(10));
        return verificadorEsperado == verificadorRecibido;
    }
}
