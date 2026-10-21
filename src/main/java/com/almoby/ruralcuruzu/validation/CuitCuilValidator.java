package com.almoby.ruralcuruzu.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Algoritmo oficial (AFIP) de dígito verificador para CUIT/CUIL:
 * se multiplica cada uno de los primeros 10 dígitos por la serie
 * 5,4,3,2,7,6,5,4,3,2, se suman los resultados, se calcula el resto
 * de dividir por 11 y el dígito verificador es 11 - resto (con los
 * casos especiales 11 -> 0 y 10 -> 9, aplicados sobre ese resultado).
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

        // Los casos especiales aplican sobre (11 - resto), no sobre el resto:
        // resto=0 da 11 -> dígito 0 y resto=1 da 10 -> dígito 9. Antes se
        // comparaba el resto contra 11/10 (imposible para 11), lo que rechazaba
        // CUITs reales con resto 0 o 1 (ej. 30-71553511-0).
        int verificadorEsperado = 11 - (suma % 11);
        if (verificadorEsperado == 11) {
            verificadorEsperado = 0;
        } else if (verificadorEsperado == 10) {
            verificadorEsperado = 9;
        }

        int verificadorRecibido = Character.getNumericValue(soloDigitos.charAt(10));
        return verificadorEsperado == verificadorRecibido;
    }
}
