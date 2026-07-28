package com.almoby.ruralcuruzu.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.almoby.ruralcuruzu.exception.ArchivoInvalidoException;

import lombok.extern.slf4j.Slf4j;

/**
 * Guarda en disco el comprobante que un socio adjunta al informar el pago de
 * una cuota por transferencia (RN-17: el pago -y su comprobante- es su
 * propia entidad, ya no queda embebido en la cuota). Mismo mecanismo y
 * mismas reglas de validación que {@link AlmacenamientoArchivosService}
 * (usado para los adjuntos de una solicitud de socio), pero en un
 * directorio propio y para un único archivo por pago en vez de una lista:
 * son dos contextos distintos (documentación de una solicitud vs.
 * comprobante de un pago) que conviene no mezclar en la misma carpeta.
 */
@Slf4j
@Service
public class AlmacenamientoComprobantesService {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of(
            "application/pdf", "image/jpeg", "image/png");

    private final Path directorioBase;

    public AlmacenamientoComprobantesService(
            @Value("${app.storage.comprobantes-pago-dir:./uploads/comprobantes-pago}") String directorioBase) {
        this.directorioBase = Path.of(directorioBase).toAbsolutePath().normalize();
    }

    /**
     * Guarda el comprobante bajo una subcarpeta con el id del pago, y
     * devuelve la ruta relativa guardada (para persistir en
     * {@code Pago.comprobanteRuta}).
     *
     * @throws ArchivoInvalidoException si el archivo no es de un tipo permitido
     */
    public String guardar(String pagoId, MultipartFile archivo) {
        String extension = extensionDe(archivo.getOriginalFilename());
        if (!EXTENSIONES_PERMITIDAS.contains(extension)
                || !CONTENT_TYPES_PERMITIDOS.contains(archivo.getContentType())) {
            throw new ArchivoInvalidoException(
                    "El archivo \"" + archivo.getOriginalFilename() + "\" no es un tipo permitido "
                            + "(solo PDF, JPG o PNG)");
        }

        String nombreSanitizado = sanitizar(archivo.getOriginalFilename());
        String nombreGuardado = UUID.randomUUID() + "_" + nombreSanitizado;
        String rutaRelativa = pagoId + "/" + nombreGuardado;

        try {
            Path carpetaPago = directorioBase.resolve(pagoId).normalize();
            // El id del pago es un ObjectId generado por Mongo, pero igual verificamos
            // que no se "escape" del directorio base antes de escribir nada en disco.
            if (!carpetaPago.startsWith(directorioBase)) {
                throw new ArchivoInvalidoException("Id de pago inválido");
            }
            Files.createDirectories(carpetaPago);

            Path destino = carpetaPago.resolve(nombreGuardado);
            try (InputStream entrada = archivo.getInputStream()) {
                Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Comprobante guardado pagoId={} archivo={}", pagoId, nombreGuardado);
            return rutaRelativa;
        } catch (IOException ex) {
            log.error("Error guardando comprobante pagoId={} nombreOriginal={}",
                    pagoId, archivo.getOriginalFilename(), ex);
            throw new IllegalStateException("No se pudo guardar el comprobante de pago", ex);
        }
    }

    /** Devuelve el archivo en disco a partir de la ruta relativa guardada, para descargarlo. */
    public Path resolverParaDescarga(String rutaRelativa) {
        Path ruta = directorioBase.resolve(rutaRelativa).normalize();
        if (!ruta.startsWith(directorioBase) || !Files.isRegularFile(ruta)) {
            throw new ArchivoInvalidoException("El comprobante solicitado no existe");
        }
        return ruta;
    }

    private String extensionDe(String nombreOriginal) {
        String extension = StringUtils.getFilenameExtension(nombreOriginal);
        return extension == null ? "" : extension.toLowerCase();
    }

    private String sanitizar(String nombreOriginal) {
        String nombre = StringUtils.getFilename(nombreOriginal) == null
                ? "archivo"
                : StringUtils.getFilename(nombreOriginal);
        return nombre.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }
}
