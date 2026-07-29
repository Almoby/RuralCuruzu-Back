package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import com.almoby.ruralcuruzu.dto.response.BeneficioMasUtilizadoResponse;
import com.almoby.ruralcuruzu.dto.response.CobranzaMensualResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoSociosResponse;
import com.almoby.ruralcuruzu.dto.response.IndicadoresPrincipalesResponse;
import com.almoby.ruralcuruzu.dto.response.UsoBeneficioPorComercioResponse;
import com.almoby.ruralcuruzu.dto.response.UsoPeriodoResponse;
import com.almoby.ruralcuruzu.service.DashboardService;

@ExtendWith(MockitoExtension.class)
class DashboardExportServiceImplTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardExportServiceImpl service;

    private IndicadoresPrincipalesResponse indicadores() {
        return new IndicadoresPrincipalesResponse(
                10, 2,
                6, 60.0,
                3,
                1,
                4, 5,
                new BigDecimal("1000"), new BigDecimal("5.5"),
                new BigDecimal("200"), 1,
                7, 20);
    }

    @Test
    void generarReportePdf_devuelveUnPdfValidoUsandoTodasLasSeccionesDelDashboard() {
        int anioActual = Year.now().getValue();

        when(dashboardService.obtenerIndicadoresPrincipales()).thenReturn(indicadores());
        when(dashboardService.obtenerCobranzaMensual(anioActual)).thenReturn(List.of(
                new CobranzaMensualResponse(anioActual + "-01", "Ene", new BigDecimal("500"), new BigDecimal("100"))));
        when(dashboardService.obtenerEstadoSocios(null, null)).thenReturn(new EstadoSociosResponse(6, 2, 1, 1));
        when(dashboardService.obtenerUsoBeneficiosPorComercio()).thenReturn(List.of(
                new UsoBeneficioPorComercioResponse("c1", "Farmacia", 10, 3, 2, "15% medicamentos",
                        List.of(new UsoPeriodoResponse(anioActual + "-01", 3L)))));
        when(dashboardService.obtenerBeneficiosMasUtilizados()).thenReturn(List.of(
                new BeneficioMasUtilizadoResponse("b1", "15% medicamentos", "Farmacia", 3)));

        byte[] pdf = service.generarReportePdf();

        assertThat(pdf).isNotEmpty();
        // Todo PDF válido empieza con la firma "%PDF-".
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        verify(dashboardService).obtenerIndicadoresPrincipales();
        verify(dashboardService).obtenerCobranzaMensual(anioActual);
        verify(dashboardService).obtenerEstadoSocios(null, null);
        verify(dashboardService).obtenerUsoBeneficiosPorComercio();
        verify(dashboardService).obtenerBeneficiosMasUtilizados();
    }

    @Test
    void generarReportePdf_encabezado_muestraFechaDeHoyConFormatoDdMmYyyy() throws java.io.IOException {
        // Approval test (characterization) for D1: pins the current dd/MM/yyyy
        // header date format before it moves to FechaUtil.FORMATO_FECHA_CORTA.
        int anioActual = Year.now().getValue();

        when(dashboardService.obtenerIndicadoresPrincipales()).thenReturn(indicadores());
        when(dashboardService.obtenerCobranzaMensual(anioActual)).thenReturn(List.of());
        when(dashboardService.obtenerEstadoSocios(null, null)).thenReturn(new EstadoSociosResponse(0, 0, 0, 0));
        when(dashboardService.obtenerUsoBeneficiosPorComercio()).thenReturn(List.of());
        when(dashboardService.obtenerBeneficiosMasUtilizados()).thenReturn(List.of());

        byte[] pdf = service.generarReportePdf();

        String fechaEsperada = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        try (PdfReader reader = new PdfReader(pdf)) {
            String textoPagina1 = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(textoPagina1).contains("Generado el " + fechaEsperada);
        }
    }

    @Test
    void generarReportePdf_sinUsosDeBeneficios_noFallaYGeneraElPdfIgual() {
        int anioActual = Year.now().getValue();

        when(dashboardService.obtenerIndicadoresPrincipales()).thenReturn(indicadores());
        when(dashboardService.obtenerCobranzaMensual(anioActual)).thenReturn(List.of());
        when(dashboardService.obtenerEstadoSocios(null, null)).thenReturn(new EstadoSociosResponse(0, 0, 0, 0));
        when(dashboardService.obtenerUsoBeneficiosPorComercio()).thenReturn(List.of());
        when(dashboardService.obtenerBeneficiosMasUtilizados()).thenReturn(List.of());

        byte[] pdf = service.generarReportePdf();

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }
}
