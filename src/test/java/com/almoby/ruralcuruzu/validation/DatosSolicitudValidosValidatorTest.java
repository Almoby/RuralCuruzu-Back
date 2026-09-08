package com.almoby.ruralcuruzu.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.almoby.ruralcuruzu.enums.TipoPersona;

import jakarta.validation.ConstraintValidatorContext;

/**
 * Punto 6 (pedido del dueño del proyecto): a diferencia de documento,
 * fechaNacimiento, nombreResponsable y dniResponsable (exclusivos de un tipo
 * de persona), cuit ahora es obligatorio SOLO para JURIDICA: una persona
 * física puede cargarse solo con DNI, sin CUIT/CUIL (algunos no lo tienen
 * registrado).
 */
class DatosSolicitudValidosValidatorTest {

    private final DatosSolicitudValidosValidator validator = new DatosSolicitudValidosValidator();

    private record Datos(
            TipoPersona tipoPersona,
            String documento,
            LocalDate fechaNacimiento,
            String nombreResponsable,
            String dniResponsable,
            String cuit
    ) implements DatosPersonaRequestValidable {
    }

    private ConstraintValidatorContext contextoFalso() {
        return mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);
    }

    @Test
    void personaFisica_sinCuit_esValida() {
        Datos datos = new Datos(TipoPersona.FISICA, "28345678", LocalDate.of(1985, 4, 12), null, null, null);

        assertThat(validator.isValid(datos, contextoFalso())).isTrue();
    }

    @Test
    void personaFisica_conCuit_tambienEsValida() {
        Datos datos = new Datos(TipoPersona.FISICA, "28345678", LocalDate.of(1985, 4, 12), null, null, "20-28345678-2");

        assertThat(validator.isValid(datos, contextoFalso())).isTrue();
    }

    @Test
    void personaJuridica_sinCuit_esInvalida() {
        Datos datos = new Datos(TipoPersona.JURIDICA, null, null, "María Fernández", "30123456", null);

        assertThat(validator.isValid(datos, contextoFalso())).isFalse();
    }

    @Test
    void personaJuridica_conCuit_esValida() {
        Datos datos = new Datos(TipoPersona.JURIDICA, null, null, "María Fernández", "30123456", "30-71234567-9");

        assertThat(validator.isValid(datos, contextoFalso())).isTrue();
    }

    @Test
    void tipoPersonaNulo_esValida_loReportaElNotNullDeTipoPersona() {
        Datos datos = new Datos(null, null, null, null, null, null);

        assertThat(validator.isValid(datos, contextoFalso())).isTrue();
    }
}
