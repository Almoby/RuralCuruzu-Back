package com.almoby.ruralcuruzu.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 20-12345678-6 y 30-30111222-6 son CUIT válidos calculados a mano con el
 * algoritmo oficial de AFIP (módulo 11, multiplicadores 5,4,3,2,7,6,5,4,3,2),
 * usados acá como casos de referencia conocidos.
 */
class CuitCuilValidatorTest {

    private final CuitCuilValidator validator = new CuitCuilValidator();

    @Test
    void aceptaCuitValidoConGuiones() {
        assertThat(validator.isValid("20-12345678-6", null)).isTrue();
    }

    @Test
    void aceptaCuitValidoSinFormato() {
        assertThat(validator.isValid("20123456786", null)).isTrue();
    }

    @Test
    void aceptaOtroCuitValidoDePersonaJuridica() {
        assertThat(validator.isValid("30-30111222-6", null)).isTrue();
    }

    @Test
    void rechazaCuitConDigitoVerificadorIncorrecto() {
        assertThat(validator.isValid("20-12345678-5", null)).isFalse();
    }

    @Test
    void rechazaCuitConCantidadDeDigitosIncorrecta() {
        assertThat(validator.isValid("20-1234-6", null)).isFalse();
    }

    @Test
    void permiteNuloOBlank_porqueNotBlankLoControlaAparte() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
    }
}
