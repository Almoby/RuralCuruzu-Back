package com.almoby.ruralcuruzu.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelefonoValidatorTest {

    private final TelefonoValidator validator = new TelefonoValidator();

    @Test
    void aceptaTelefonoConPrefijoInternacionalYEspacios() {
        assertThat(validator.isValid("+54 9 3777 123456", null)).isTrue();
    }

    @Test
    void aceptaTelefonoSoloDigitos() {
        assertThat(validator.isValid("37771234567", null)).isTrue();
    }

    @Test
    void rechazaTelefonoDemasiadoCorto() {
        assertThat(validator.isValid("12345", null)).isFalse();
    }

    @Test
    void rechazaTelefonoConLetras() {
        assertThat(validator.isValid("3777abc456", null)).isFalse();
    }

    @Test
    void permiteNuloOBlank_porqueNotBlankLoControlaAparte() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
    }
}
