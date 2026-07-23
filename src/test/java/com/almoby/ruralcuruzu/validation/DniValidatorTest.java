package com.almoby.ruralcuruzu.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DniValidatorTest {

    private final DniValidator validator = new DniValidator();

    @Test
    void aceptaDniDeOchoDigitos() {
        assertThat(validator.isValid("12345678", null)).isTrue();
    }

    @Test
    void aceptaDniDeSieteDigitos() {
        assertThat(validator.isValid("1234567", null)).isTrue();
    }

    @Test
    void aceptaDniConPuntosDeMiles() {
        assertThat(validator.isValid("28.345.678", null)).isTrue();
    }

    @Test
    void rechazaDniConLetras() {
        assertThat(validator.isValid("1234567a", null)).isFalse();
    }

    @Test
    void rechazaDniDemasiadoLargo() {
        assertThat(validator.isValid("123456789", null)).isFalse();
    }

    @Test
    void permiteNuloOBlank_porqueNotBlankLoControlaAparte() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
    }
}
