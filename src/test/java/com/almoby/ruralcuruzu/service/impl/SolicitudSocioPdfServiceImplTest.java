package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import com.almoby.ruralcuruzu.domain.DatosPersonaFisica;
import com.almoby.ruralcuruzu.domain.DatosPersonaJuridica;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoSolicitud;
import com.almoby.ruralcuruzu.enums.TipoPersona;

class SolicitudSocioPdfServiceImplTest {

    private static final byte[] FIRMA_PDF = {'%', 'P', 'D', 'F', '-'};

    private SolicitudSocioPdfServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SolicitudSocioPdfServiceImpl();
    }

    @Test
    void generarPdf_personaFisica_devuelveUnPdfValidoConLosDatos() throws java.io.IOException {
        DatosPersonaFisica fisica = new DatosPersonaFisica(
                "García, Juan Carlos", "28345678", LocalDate.of(1985, 4, 12), "20-28345678-2",
                "Calle 123", "Depto B", "+54 9 3777123456", "socio@example.com", "Farmacia Central",
                "Ruta 123 km 4");
        SolicitudSocioResponse solicitud = new SolicitudSocioResponse(
                "SOL-000123", EstadoSolicitud.PENDIENTE, CategoriaSocio.ACTIVO, TipoPersona.FISICA,
                fisica, null, Instant.now(), Instant.now(), List.of());

        byte[] pdf = service.generarPdf(solicitud);

        assertThat(pdf).isNotEmpty();
        assertThat(firmaPdf(pdf)).isTrue();
        try (PdfReader reader = new PdfReader(pdf)) {
            String texto = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(texto).contains("SOL-000123");
            assertThat(texto).contains("García, Juan Carlos");
            assertThat(texto).contains("28345678");
            assertThat(texto).contains("Pendiente");
            assertThat(texto).contains("Socio activo");
            assertThat(texto).contains("Persona física");
        }
    }

    @Test
    void generarPdf_personaJuridica_devuelveUnPdfValidoConLosDatos() throws java.io.IOException {
        DatosPersonaJuridica juridica = new DatosPersonaJuridica(
                "Comercial del Sur SA", "30-71234567-9", "Calle 123", null, "+54 9 3777123456",
                "contacto@comercialdelsur.com", "Depósito", "María Fernández", "30123456", "Ruta 123 km 4");
        SolicitudSocioResponse solicitud = new SolicitudSocioResponse(
                "SOL-000124", EstadoSolicitud.APROBADA, CategoriaSocio.ADHERENTE, TipoPersona.JURIDICA,
                null, juridica, Instant.now(), Instant.now(), List.of());

        byte[] pdf = service.generarPdf(solicitud);

        assertThat(pdf).isNotEmpty();
        assertThat(firmaPdf(pdf)).isTrue();
        try (PdfReader reader = new PdfReader(pdf)) {
            String texto = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(texto).contains("SOL-000124");
            assertThat(texto).contains("Comercial del Sur SA");
            assertThat(texto).contains("30-71234567-9");
            assertThat(texto).contains("Aprobada");
            assertThat(texto).contains("Socio adherente");
            assertThat(texto).contains("Persona jurídica");
        }
    }

    @Test
    void generarPdf_sinDatosDePersonaNiFechas_noFallaYUsaGuion() throws java.io.IOException {
        SolicitudSocioResponse solicitud = new SolicitudSocioResponse(
                null, null, null, TipoPersona.FISICA,
                null, null, null, null, List.of());

        byte[] pdf = service.generarPdf(solicitud);

        assertThat(pdf).isNotEmpty();
        assertThat(firmaPdf(pdf)).isTrue();
        try (PdfReader reader = new PdfReader(pdf)) {
            String texto = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(texto).contains("-");
        }
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
