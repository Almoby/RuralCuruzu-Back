package com.almoby.ruralcuruzu.service.impl;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.dto.response.PagoResponse;
import com.almoby.ruralcuruzu.service.ComprobantePagoPdfService;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Ver ComprobantePagoPdfService. Mismo enfoque de maquetado que
 * DashboardExportServiceImpl (OpenPDF, package com.lowagie.text): un único
 * método arma el documento completo, sin ninguna regla de negocio nueva.
 */
@Service
public class ComprobantePagoPdfServiceImpl implements ComprobantePagoPdfService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font FUENTE_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FUENTE_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font FUENTE_SECCION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font FUENTE_CELDA = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font FUENTE_PIE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

    @Override
    public byte[] generarConstancia(PagoResponse pago) {
        Document documento = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            agregarEncabezado(documento);
            agregarDatosDelPago(documento, pago);
            agregarPie(documento);

            documento.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar la constancia de pago en PDF", ex);
        }

        return salida.toByteArray();
    }

    private void agregarEncabezado(Document documento) throws DocumentException {
        Paragraph titulo = new Paragraph("Rural Curuzú - Constancia de pago", FUENTE_TITULO);
        titulo.setSpacingAfter(2);
        documento.add(titulo);

        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        Paragraph subtitulo = new Paragraph("Generado el " + fecha, FUENTE_SUBTITULO);
        subtitulo.setSpacingAfter(16);
        documento.add(subtitulo);
    }

    private void agregarDatosDelPago(Document documento, PagoResponse pago) throws DocumentException {
        Paragraph seccion = new Paragraph("Datos del pago", FUENTE_SECCION);
        seccion.setSpacingAfter(6);
        documento.add(seccion);

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[] {40, 60});
        tabla.setSpacingAfter(10);

        agregarFila(tabla, "Socio", valorOGuion(pago.socioNombre())
                + (pago.socioNumeroSocio() != null ? " (Nº " + pago.socioNumeroSocio() + ")" : ""));
        agregarFila(tabla, "Período", valorOGuion(pago.periodo()));
        agregarFila(tabla, "Importe", formatoMoneda(pago.importe()));
        agregarFila(tabla, "Medio de pago", formatoMedioPago(pago.medioPago()));
        agregarFila(tabla, "Estado", formatoEstado(pago.estado()));
        agregarFila(tabla, "Fecha de pago", formatoFecha(pago.fechaPago()));
        if (pago.registradoPorAdminNombre() != null) {
            agregarFila(tabla, "Registrado por", pago.registradoPorAdminNombre());
        }
        agregarFila(tabla, "Nº de comprobante", valorOGuion(pago.id()));

        documento.add(tabla);
    }

    private void agregarPie(Document documento) throws DocumentException {
        Paragraph pie = new Paragraph(
                "Constancia generada automáticamente por el sistema, ya que este pago no cuenta con un archivo de "
                        + "comprobante adjunto (registrado directamente por la cooperativa o pagado a través de "
                        + "Mercado Pago).",
                FUENTE_PIE);
        pie.setSpacingBefore(20);
        documento.add(pie);
    }

    private void agregarFila(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Paragraph(etiqueta, FUENTE_CELDA));
        celdaEtiqueta.setPadding(6);
        celdaEtiqueta.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Paragraph(valor, FUENTE_CELDA));
        celdaValor.setPadding(6);
        celdaValor.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabla.addCell(celdaValor);
    }

    private String formatoMedioPago(com.almoby.ruralcuruzu.enums.MedioPago medioPago) {
        if (medioPago == null) {
            return "-";
        }
        return switch (medioPago) {
            case EFECTIVO -> "Efectivo";
            case VENTANILLA -> "Ventanilla";
            case TRANSFERENCIA -> "Transferencia";
            case DEBITO -> "Débito";
            case LINK_DE_PAGO -> "Link de pago (Mercado Pago)";
        };
    }

    private String formatoEstado(com.almoby.ruralcuruzu.enums.EstadoPago estado) {
        if (estado == null) {
            return "-";
        }
        return switch (estado) {
            case EN_REVISION -> "En revisión";
            case APROBADO -> "Aprobado";
            case RECHAZADO -> "Rechazado";
        };
    }

    private String formatoFecha(Instant instante) {
        if (instante == null) {
            return "-";
        }
        return instante.atZone(ZoneId.systemDefault()).format(FORMATO_FECHA);
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
