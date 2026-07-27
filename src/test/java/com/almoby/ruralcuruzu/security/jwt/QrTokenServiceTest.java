package com.almoby.ruralcuruzu.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.almoby.ruralcuruzu.exception.CodigoQrExpiradoException;
import com.almoby.ruralcuruzu.exception.CodigoQrInvalidoException;

/**
 * Ver documento, sección 15.1: el QR "no deberá utilizarse como una imagen
 * fija permanente [...] deberá renovarse automáticamente". Estos tests
 * cubren el ciclo de vida del token que lo reemplaza: generación, vencimiento
 * real por tiempo, y rechazo si está manipulado.
 */
class QrTokenServiceTest {

    private static final String SECRETO_TEST = "test-only-secret-do-not-use-in-production-1234567890";

    private QrTokenService service;

    @BeforeEach
    void setUp() {
        service = new QrTokenService(SECRETO_TEST, 60);
    }

    @Test
    void generar_devuelveUnTokenQueExpiraEnElFuturo() {
        Instant antes = Instant.now();

        QrTokenGenerado generado = service.generar("socio-1");

        assertThat(generado.token()).isNotBlank();
        assertThat(generado.expiraEn()).isAfter(antes);
    }

    @Test
    void generarYExtraerSocioId_devuelveElMismoIdConElQueSeGenero() {
        QrTokenGenerado generado = service.generar("socio-1");

        String socioId = service.extraerSocioId(generado.token());

        assertThat(socioId).isEqualTo("socio-1");
    }

    @Test
    void generar_dosLlamadasSeguidasDevuelvenTokensDistintos() {
        QrTokenGenerado primero = service.generar("socio-1");
        QrTokenGenerado segundo = service.generar("socio-1");

        assertThat(primero.token()).isNotEqualTo(segundo.token());
    }

    @Test
    void extraerSocioId_conTokenVencido_lanzaCodigoQrExpirado() {
        QrTokenService serviceDeVidaCorta = new QrTokenService(SECRETO_TEST, -1);

        QrTokenGenerado generado = serviceDeVidaCorta.generar("socio-1");

        assertThatThrownBy(() -> serviceDeVidaCorta.extraerSocioId(generado.token()))
                .isInstanceOf(CodigoQrExpiradoException.class);
    }

    @Test
    void extraerSocioId_conTokenManipulado_lanzaCodigoQrInvalido() {
        QrTokenGenerado generado = service.generar("socio-1");
        String tokenManipulado = generado.token() + "manipulado";

        assertThatThrownBy(() -> service.extraerSocioId(tokenManipulado))
                .isInstanceOf(CodigoQrInvalidoException.class);
    }

    @Test
    void extraerSocioId_conTokenFirmadoPorOtraClave_lanzaCodigoQrInvalido() {
        QrTokenService otroServicio = new QrTokenService("otro-secreto-completamente-distinto-1234567890", 60);
        QrTokenGenerado generado = otroServicio.generar("socio-1");

        assertThatThrownBy(() -> service.extraerSocioId(generado.token()))
                .isInstanceOf(CodigoQrInvalidoException.class);
    }

    @Test
    void validezSegundos_devuelveElValorConfigurado() {
        assertThat(service.validezSegundos()).isEqualTo(60);
    }
}
