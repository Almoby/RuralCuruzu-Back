package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.almoby.ruralcuruzu.dto.response.PagoResponse;
import com.almoby.ruralcuruzu.enums.EstadoPago;
import com.almoby.ruralcuruzu.enums.MedioPago;

class ComprobantePagoPdfServiceImplTest {

    private static final byte[] FIRMA_PDF = {'%', 'P', 'D', 'F', '-'};

    private ComprobantePagoPdfServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ComprobantePagoPdfServiceImpl();
    }

    @Test
    void generarConstancia_pagoCompleto_devuelveUnPdfValido() {
        PagoResponse pago = new PagoResponse(
                "pago-1", "cuota-1", "S-100", "Juan Pérez", "2026-07",
                BigDecimal.valueOf(3500), MedioPago.VENTANILLA, EstadoPago.APROBADO,
                Instant.now(), null, "Pago en efectivo en oficina", false,
                "Admin Uno", null, Instant.now());

        byte[] pdf = service.generarConstancia(pago);

        assertThat(pdf).isNotEmpty();
        assertThat(firmaPdf(pdf)).isTrue();
    }

    @Test
    void generarConstancia_pagoMercadoPagoConDatosNulos_noFalla() {
        PagoResponse pago = new PagoResponse(
                "pago-2", "cuota-2", null, null, null,
                null, MedioPago.LINK_DE_PAGO, EstadoPago.APROBADO,
                null, null, null, true,
                null, null, Instant.now());

        byte[] pdf = service.generarConstancia(pago);

        assertThat(pdf).isNotEmpty();
        assertThat(firmaPdf(pdf)).isTrue();
    }

    private boolean firmaPdf(byte[] pdf) {
        if (pdf.length < FIRMA_PDF.length) {
            return false;
        }
        for (int i = 0; i < FIRMA_PDF.length; i++) {
            if (pdf[i] != FIRMA_PDF[i]) {
                return false;
            }
        }
        return true;
    }
}
