package com.almoby.ruralcuruzu.service.impl;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.dto.response.BeneficioMasUtilizadoResponse;
import com.almoby.ruralcuruzu.dto.response.CobranzaMensualPorCategoriaResponse;
import com.almoby.ruralcuruzu.dto.response.CobranzaMensualResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoSociosResponse;
import com.almoby.ruralcuruzu.dto.response.IndicadoresPrincipalesResponse;
import com.almoby.ruralcuruzu.dto.response.SocioConDeudaResponse;
import com.almoby.ruralcuruzu.dto.response.UsoBeneficioPorComercioResponse;
import com.almoby.ruralcuruzu.service.DashboardExportService;
import com.almoby.ruralcuruzu.service.DashboardService;
import com.almoby.ruralcuruzu.util.FechaUtil;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Ver DashboardExportService. Todo el layout del PDF vive acá, adentro de un
 * único método por sección: no hay ninguna regla de negocio nueva, solo se
 * vuelca en tablas lo que DashboardService ya calcula.
 */
@Service
public class DashboardExportServiceImpl implements DashboardExportService {

    private static final Font FUENTE_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FUENTE_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font FUENTE_SECCION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font FUENTE_ENCABEZADO_TABLA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font FUENTE_CELDA = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Color GRIS_ENCABEZADO = new Color(230, 230, 230);

    private final DashboardService dashboardService;

    public DashboardExportServiceImpl(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Override
    public byte[] generarReportePdf() {
        Document documento = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            int anioActual = Year.now().getValue();
            agregarEncabezado(documento);
            agregarIndicadoresPrincipales(documento, dashboardService.obtenerIndicadoresPrincipales());
            agregarCobranzaMensual(documento, dashboardService.obtenerCobranzaMensual(anioActual));
            agregarCobranzaMensualPorCategoria(documento, dashboardService.obtenerCobranzaMensualPorCategoria(anioActual));
            agregarEstadoSocios(documento, dashboardService.obtenerEstadoSocios(null, null));
            agregarSociosConDeuda(documento, dashboardService.obtenerSociosConDeuda());
            agregarUsoPorComercio(documento, dashboardService.obtenerUsoBeneficiosPorComercio());
            agregarBeneficiosMasUtilizados(documento, dashboardService.obtenerBeneficiosMasUtilizados());

            documento.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el PDF del reporte de dashboard", ex);
        }

        return salida.toByteArray();
    }

    private void agregarEncabezado(Document documento) throws DocumentException {
        Paragraph titulo = new Paragraph("Rural Curuzú - Reporte de la cooperativa", FUENTE_TITULO);
        titulo.setSpacingAfter(2);
        documento.add(titulo);

        String fecha = LocalDate.now().format(FechaUtil.FORMATO_FECHA_CORTA);
        Paragraph subtitulo = new Paragraph("Generado el " + fecha, FUENTE_SUBTITULO);
        subtitulo.setSpacingAfter(16);
        documento.add(subtitulo);
    }

    private void agregarIndicadoresPrincipales(Document documento, IndicadoresPrincipalesResponse indicadores)
            throws DocumentException {
        agregarTituloSeccion(documento, "Indicadores principales");

        PdfPTable tabla = nuevaTabla(2, 60, 40);
        agregarFilaEtiquetaValor(tabla, "Total de socios", String.valueOf(indicadores.totalSocios()));
        agregarFilaEtiquetaValor(tabla, "Socios activos", String.valueOf(indicadores.sociosActivos()));
        agregarFilaEtiquetaValor(tabla, "Socios nuevos este mes", String.valueOf(indicadores.sociosNuevosEsteMes()));
        agregarFilaEtiquetaValor(tabla, "Socios nuevos este año", String.valueOf(indicadores.sociosNuevosEsteAnio()));
        agregarFilaEtiquetaValor(tabla, "Socios con cuota al día", String.valueOf(indicadores.sociosConCuotaAlDia()));
        agregarFilaEtiquetaValor(tabla, "Socios con cuota pendiente", String.valueOf(indicadores.sociosConCuotaPendiente()));
        agregarFilaEtiquetaValor(tabla, "Socios con cuota vencida", String.valueOf(indicadores.sociosConCuotaVencida()));
        agregarFilaEtiquetaValor(tabla, "Comercios activos", String.valueOf(indicadores.comerciosActivos()));
        agregarFilaEtiquetaValor(tabla, "Promociones activas", String.valueOf(indicadores.promocionesActivas()));
        agregarFilaEtiquetaValor(tabla, "Facturación mensual", formatoMoneda(indicadores.facturacionMensual()));
        agregarFilaEtiquetaValor(tabla, "Deuda acumulada (cuotas vencidas)", formatoMoneda(indicadores.deudaAcumulada()));
        agregarFilaEtiquetaValor(tabla, "Socios en mora", String.valueOf(indicadores.sociosEnMora()));
        agregarFilaEtiquetaValor(tabla, "Beneficios utilizados este mes", String.valueOf(indicadores.beneficiosUtilizados()));
        agregarFilaEtiquetaValor(tabla, "Beneficios utilizados (histórico)",
                String.valueOf(indicadores.beneficiosUtilizadosHistoricoTotal()));

        documento.add(tabla);
    }

    private void agregarCobranzaMensual(Document documento, List<CobranzaMensualResponse> cobranza)
            throws DocumentException {
        agregarTituloSeccion(documento, "Cobranza mensual - " + Year.now().getValue());

        PdfPTable tabla = nuevaTabla(3, 34, 33, 33);
        agregarEncabezados(tabla, "Mes", "Cobrado", "Pendiente");
        for (CobranzaMensualResponse fila : cobranza) {
            agregarCelda(tabla, fila.mes(), Element.ALIGN_LEFT);
            agregarCelda(tabla, formatoMoneda(fila.cobrado()), Element.ALIGN_RIGHT);
            agregarCelda(tabla, formatoMoneda(fila.pendiente()), Element.ALIGN_RIGHT);
        }
        documento.add(tabla);
    }

    private void agregarCobranzaMensualPorCategoria(Document documento, List<CobranzaMensualPorCategoriaResponse> cobranza)
            throws DocumentException {
        agregarTituloSeccion(documento, "Cuotas cobradas por mes - socios activos y adherentes");

        PdfPTable tabla = nuevaTabla(3, 34, 33, 33);
        agregarEncabezados(tabla, "Mes", "Cobrado (Activo)", "Cobrado (Adherente)");
        for (CobranzaMensualPorCategoriaResponse fila : cobranza) {
            agregarCelda(tabla, fila.mes(), Element.ALIGN_LEFT);
            agregarCelda(tabla, formatoMoneda(fila.cobradoActivo()), Element.ALIGN_RIGHT);
            agregarCelda(tabla, formatoMoneda(fila.cobradoAdherente()), Element.ALIGN_RIGHT);
        }
        documento.add(tabla);
    }

    private void agregarSociosConDeuda(Document documento, List<SocioConDeudaResponse> sociosConDeuda)
            throws DocumentException {
        agregarTituloSeccion(documento, "Deuda acumulada por socio");

        if (sociosConDeuda.isEmpty()) {
            documento.add(new Paragraph("No hay socios con cuotas vencidas.", FUENTE_CELDA));
            return;
        }

        PdfPTable tabla = nuevaTabla(4, 15, 40, 25, 20);
        agregarEncabezados(tabla, "N° socio", "Nombre", "Monto adeudado", "Cuotas vencidas");
        for (SocioConDeudaResponse fila : sociosConDeuda) {
            agregarCelda(tabla, valorOGuion(fila.numeroSocio()), Element.ALIGN_LEFT);
            agregarCelda(tabla, valorOGuion(fila.nombre()), Element.ALIGN_LEFT);
            agregarCelda(tabla, formatoMoneda(fila.montoAdeudado()), Element.ALIGN_RIGHT);
            agregarCelda(tabla, String.valueOf(fila.cantidadCuotasVencidas()), Element.ALIGN_RIGHT);
        }
        documento.add(tabla);
    }

    private void agregarEstadoSocios(Document documento, EstadoSociosResponse estado) throws DocumentException {
        agregarTituloSeccion(documento, "Estado de socios");

        PdfPTable tabla = nuevaTabla(2, 60, 40);
        agregarFilaEtiquetaValor(tabla, "Al día", String.valueOf(estado.alDia()));
        agregarFilaEtiquetaValor(tabla, "Pendientes", String.valueOf(estado.pendientes()));
        agregarFilaEtiquetaValor(tabla, "Vencidos", String.valueOf(estado.vencidos()));
        agregarFilaEtiquetaValor(tabla, "Inactivos", String.valueOf(estado.inactivos()));
        documento.add(tabla);
    }

    private void agregarUsoPorComercio(Document documento, List<UsoBeneficioPorComercioResponse> usos)
            throws DocumentException {
        agregarTituloSeccion(documento, "Uso de beneficios por comercio");

        if (usos.isEmpty()) {
            documento.add(new Paragraph("Todavía no hay usos de beneficios registrados.", FUENTE_CELDA));
            return;
        }

        PdfPTable tabla = nuevaTabla(5, 28, 15, 15, 14, 28);
        agregarEncabezados(tabla, "Comercio", "Usos (histórico)", "Usos (este mes)", "Socios únicos", "Promoción más usada");
        for (UsoBeneficioPorComercioResponse fila : usos) {
            agregarCelda(tabla, fila.comercioNombre(), Element.ALIGN_LEFT);
            agregarCelda(tabla, String.valueOf(fila.cantidadBeneficiosUtilizados()), Element.ALIGN_RIGHT);
            agregarCelda(tabla, String.valueOf(fila.cantidadBeneficiosUtilizadosEsteMes()), Element.ALIGN_RIGHT);
            agregarCelda(tabla, String.valueOf(fila.cantidadSociosUnicos()), Element.ALIGN_RIGHT);
            agregarCelda(tabla, valorOGuion(fila.promocionMasUtilizada()), Element.ALIGN_LEFT);
        }
        documento.add(tabla);
    }

    private void agregarBeneficiosMasUtilizados(Document documento, List<BeneficioMasUtilizadoResponse> beneficios)
            throws DocumentException {
        agregarTituloSeccion(documento, "Beneficios más utilizados (mes actual)");

        if (beneficios.isEmpty()) {
            documento.add(new Paragraph("Todavía no hay usos de beneficios registrados este mes.", FUENTE_CELDA));
            return;
        }

        PdfPTable tabla = nuevaTabla(3, 45, 35, 20);
        agregarEncabezados(tabla, "Beneficio", "Comercio", "Usos este mes");
        for (BeneficioMasUtilizadoResponse fila : beneficios) {
            agregarCelda(tabla, valorOGuion(fila.beneficioTitulo()), Element.ALIGN_LEFT);
            agregarCelda(tabla, valorOGuion(fila.comercioNombre()), Element.ALIGN_LEFT);
            agregarCelda(tabla, String.valueOf(fila.usosEsteMes()), Element.ALIGN_RIGHT);
        }
        documento.add(tabla);
    }

    private void agregarTituloSeccion(Document documento, String texto) throws DocumentException {
        Paragraph titulo = new Paragraph(texto, FUENTE_SECCION);
        titulo.setSpacingBefore(14);
        titulo.setSpacingAfter(6);
        documento.add(titulo);
    }

    private PdfPTable nuevaTabla(int columnas, float... anchosRelativos) throws DocumentException {
        PdfPTable tabla = new PdfPTable(columnas);
        tabla.setWidthPercentage(100);
        if (anchosRelativos.length == columnas) {
            tabla.setWidths(anchosRelativos);
        }
        tabla.setSpacingBefore(2);
        tabla.setSpacingAfter(10);
        return tabla;
    }

    private void agregarEncabezados(PdfPTable tabla, String... encabezados) {
        for (String encabezado : encabezados) {
            PdfPCell celda = new PdfPCell(new Phrase(encabezado, FUENTE_ENCABEZADO_TABLA));
            celda.setPadding(5);
            celda.setBackgroundColor(GRIS_ENCABEZADO);
            tabla.addCell(celda);
        }
    }

    private void agregarCelda(PdfPTable tabla, String texto, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FUENTE_CELDA));
        celda.setPadding(5);
        celda.setHorizontalAlignment(alineacion);
        tabla.addCell(celda);
    }

    private void agregarFilaEtiquetaValor(PdfPTable tabla, String etiqueta, String valor) {
        agregarCelda(tabla, etiqueta, Element.ALIGN_LEFT);
        agregarCelda(tabla, valor, Element.ALIGN_RIGHT);
    }

    private String valorOGuion(String valor) {
        return valor != null && !valor.isBlank() ? valor : "-";
    }

    private String formatoMoneda(BigDecimal monto) {
        if (monto == null) {
            return "-";
        }
        return "$ " + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
