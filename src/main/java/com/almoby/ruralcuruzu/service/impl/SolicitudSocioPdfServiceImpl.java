package com.almoby.ruralcuruzu.service.impl;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.DatosPersonaFisica;
import com.almoby.ruralcuruzu.domain.DatosPersonaJuridica;
import com.almoby.ruralcuruzu.dto.response.SolicitudSocioResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoSolicitud;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.service.SolicitudSocioPdfService;
import com.almoby.ruralcuruzu.util.FechaUtil;
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
 * Ver SolicitudSocioPdfService. Mismo enfoque de maquetado que
 * ComprobantePagoPdfServiceImpl/DashboardExportServiceImpl (OpenPDF, package
 * com.lowagie.text): un único método arma el documento completo, sin
 * ninguna regla de negocio nueva.
 */
@Service
public class SolicitudSocioPdfServiceImpl implements SolicitudSocioPdfService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font FUENTE_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FUENTE_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font FUENTE_SECCION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font FUENTE_CELDA = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font FUENTE_PIE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

    @Override
    public byte[] generarPdf(SolicitudSocioResponse solicitud) {
        Document documento = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            agregarEncabezado(documento, solicitud);
            agregarDatosGenerales(documento, solicitud);
            if (solicitud.tipoPersona() == TipoPersona.FISICA && solicitud.datosPersonaFisica() != null) {
                agregarDatosPersonaFisica(documento, solicitud.datosPersonaFisica());
            } else if (solicitud.tipoPersona() == TipoPersona.JURIDICA && solicitud.datosPersonaJuridica() != null) {
                agregarDatosPersonaJuridica(documento, solicitud.datosPersonaJuridica());
            }
            agregarPie(documento);

            documento.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el PDF de la solicitud de socio", ex);
        }

        return salida.toByteArray();
    }

    private void agregarEncabezado(Document documento, SolicitudSocioResponse solicitud) throws DocumentException {
        Paragraph titulo = new Paragraph("Rural Curuzú - Solicitud de adhesión", FUENTE_TITULO);
        titulo.setSpacingAfter(2);
        documento.add(titulo);

        String fecha = LocalDate.now().format(FechaUtil.FORMATO_FECHA_CORTA);
        Paragraph subtitulo = new Paragraph(
                "Solicitud " + valorOGuion(solicitud.numeroSolicitud()) + " - Generado el " + fecha,
                FUENTE_SUBTITULO);
        subtitulo.setSpacingAfter(16);
        documento.add(subtitulo);
    }

    private void agregarDatosGenerales(Document documento, SolicitudSocioResponse solicitud) throws DocumentException {
        Paragraph seccion = new Paragraph("Datos de la solicitud", FUENTE_SECCION);
        seccion.setSpacingAfter(6);
        documento.add(seccion);

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[] {40, 60});
        tabla.setSpacingAfter(10);

        agregarFila(tabla, "Número de solicitud", valorOGuion(solicitud.numeroSolicitud()));
        agregarFila(tabla, "Estado", formatoEstado(solicitud.estado()));
        agregarFila(tabla, "Categoría solicitada", formatoCategoria(solicitud.categoriaSolicitada()));
        agregarFila(tabla, "Tipo de persona", formatoTipoPersona(solicitud.tipoPersona()));
        agregarFila(tabla, "Fecha de solicitud", formatoFecha(solicitud.fechaCreacion()));
        agregarFila(tabla, "Última actualización", formatoFecha(solicitud.fechaActualizacion()));

        documento.add(tabla);
    }

    private void agregarDatosPersonaFisica(Document documento, DatosPersonaFisica datos) throws DocumentException {
        Paragraph seccion = new Paragraph("Datos del solicitante (persona física)", FUENTE_SECCION);
        seccion.setSpacingAfter(6);
        documento.add(seccion);

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[] {40, 60});
        tabla.setSpacingAfter(10);

        agregarFila(tabla, "Apellido y nombre", valorOGuion(datos.getApellidoYNombre()));
        agregarFila(tabla, "DNI", valorOGuion(datos.getDni()));
        agregarFila(tabla, "Fecha de nacimiento", datos.getFechaNacimiento() != null
                ? datos.getFechaNacimiento().format(FechaUtil.FORMATO_FECHA_CORTA) : "-");
        agregarFila(tabla, "CUIT/CUIL", valorOGuion(datos.getCuitCuil()));
        agregarFila(tabla, "Dirección", direccionCompleta(datos.getDireccion(), datos.getPortalPisoDepartamento()));
        agregarFila(tabla, "Teléfono", valorOGuion(datos.getTelefono()));
        agregarFila(tabla, "Correo electrónico", valorOGuion(datos.getCorreoElectronico()));
        agregarFila(tabla, "Establecimiento u ocupación", valorOGuion(datos.getNombreEstablecimiento()));
        agregarFila(tabla, "Dirección del establecimiento", valorOGuion(datos.getDireccionEstablecimiento()));

        documento.add(tabla);
    }

    private void agregarDatosPersonaJuridica(Document documento, DatosPersonaJuridica datos) throws DocumentException {
        Paragraph seccion = new Paragraph("Datos del solicitante (persona jurídica)", FUENTE_SECCION);
        seccion.setSpacingAfter(6);
        documento.add(seccion);

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[] {40, 60});
        tabla.setSpacingAfter(10);

        agregarFila(tabla, "Razón social", valorOGuion(datos.getRazonSocial()));
        agregarFila(tabla, "CUIT", valorOGuion(datos.getCuit()));
        agregarFila(tabla, "Dirección", direccionCompleta(datos.getDireccion(), datos.getPortalPisoDepartamento()));
        agregarFila(tabla, "Teléfono", valorOGuion(datos.getTelefono()));
        agregarFila(tabla, "Correo electrónico", valorOGuion(datos.getCorreoElectronico()));
        agregarFila(tabla, "Establecimiento", valorOGuion(datos.getNombreEstablecimiento()));
        agregarFila(tabla, "Dirección del establecimiento", valorOGuion(datos.getDireccionEstablecimiento()));
        agregarFila(tabla, "Responsable", valorOGuion(datos.getNombreResponsable()));
        agregarFila(tabla, "DNI del responsable", valorOGuion(datos.getDniResponsable()));

        documento.add(tabla);
    }

    private void agregarPie(Document documento) throws DocumentException {
        Paragraph pie = new Paragraph(
                "Documento generado automáticamente por el sistema a partir de los datos declarados por el "
                        + "solicitante. No reemplaza la documentación respaldatoria que se haya adjuntado.",
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

    private String direccionCompleta(String direccion, String portalPisoDepartamento) {
        if (direccion == null || direccion.isBlank()) {
            return "-";
        }
        return portalPisoDepartamento != null && !portalPisoDepartamento.isBlank()
                ? direccion + " (" + portalPisoDepartamento + ")"
                : direccion;
    }

    private String formatoEstado(EstadoSolicitud estado) {
        if (estado == null) {
            return "-";
        }
        return switch (estado) {
            case PENDIENTE -> "Pendiente";
            case EN_REVISION -> "En revisión";
            case APROBADA -> "Aprobada";
            case RECHAZADA -> "Rechazada";
            case CANCELADA -> "Cancelada";
        };
    }

    private String formatoCategoria(CategoriaSocio categoria) {
        if (categoria == null) {
            return "-";
        }
        return switch (categoria) {
            case ACTIVO -> "Socio activo";
            case ADHERENTE -> "Socio adherente";
        };
    }

    private String formatoTipoPersona(TipoPersona tipoPersona) {
        if (tipoPersona == null) {
            return "-";
        }
        return switch (tipoPersona) {
            case FISICA -> "Persona física";
            case JURIDICA -> "Persona jurídica";
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
}
