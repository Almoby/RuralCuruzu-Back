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

    /** Suma 143 -> resto 0 -> 11 - 0 = 11 -> dígito 0. Antes se rechazaba (bug reportado en la Rural). */
    @Test
    void aceptaCuitConRestoCero_digitoVerificadorCero() {
        assertThat(validator.isValid("30-71553511-0", null)).isTrue();
        assertThat(validator.isValid("30715535110", null)).isTrue();
    }

    /** Suma 144 -> resto 1 -> 11 - 1 = 10 -> dígito 9. */
    @Test
    void aceptaCuitConRestoUno_digitoVerificadorNueve() {
        assertThat(validator.isValid("20-12345676-9", null)).isTrue();
    }

    @Test
    void aceptaCuitFisicoConRestoDos() {
        assertThat(validator.isValid("20-41173662-9", null)).isTrue();
        assertThat(validator.isValid("20411736629", null)).isTrue();
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
