package com.almoby.ruralcuruzu.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintValidatorContext;

class RangoDeVigenciaValidoValidatorTest {

    private final RangoDeVigenciaValidoValidator validator = new RangoDeVigenciaValidoValidator();

    private record Rango(LocalDate fechaInicioVigencia, LocalDate fechaFinVigencia) implements RangoDeVigencia {
    }

    @Test
    void aceptaCuandoFinEsPosteriorAInicio() {
        Rango rango = new Rango(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertThat(validator.isValid(rango, mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS))).isTrue();
    }

    @Test
    void aceptaCuandoFinEsIgualAInicio() {
        Rango rango = new Rango(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));
        assertThat(validator.isValid(rango, mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS))).isTrue();
    }

    @Test
    void aceptaCuandoAmbasFechasSonNulas() {
        Rango rango = new Rango(null, null);
        assertThat(validator.isValid(rango, mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS))).isTrue();
    }

    @Test
    void aceptaCuandoSoloUnaFechaEstaPresente() {
        Rango soloInicio = new Rango(LocalDate.of(2026, 1, 1), null);
        Rango soloFin = new Rango(null, LocalDate.of(2026, 1, 1));

        assertThat(validator.isValid(soloInicio, mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS))).isTrue();
        assertThat(validator.isValid(soloFin, mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS))).isTrue();
    }

    @Test
    void rechazaCuandoFinEsAnteriorAInicio() {
        Rango rango = new Rango(LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1));
        assertThat(validator.isValid(rango, mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS))).isFalse();
    }
}
