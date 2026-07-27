package com.almoby.ruralcuruzu.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.almoby.ruralcuruzu.exception.ArchivoInvalidoException;

import lombok.extern.slf4j.Slf4j;

/**
 * Guarda archivos adjuntos (documentación de solicitudes) en disco local del
 * propio servidor, no en un servicio externo: alcanza para el tamaño de esta
 * cooperativa y evita sumar una dependencia/costo nuevo. Sin interfaz propia,
 * igual que TokenRespuestaSolicitudService: solo va a haber una implementación.
 */
@Slf4j
@Service
public class AlmacenamientoArchivosService {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of(
            "application/pdf", "image/jpeg", "image/png");

    private final Path directorioBase;

    public AlmacenamientoArchivosService(
            @Value("${app.storage.solicitudes-adjuntos-dir:./uploads/solicitudes-socio}") String directorioBase) {
        this.directorioBase = Path.of(directorioBase).toAbsolutePath().normalize();
    }

    /**
     * Guarda todos los archivos bajo una subcarpeta con el número de
     * solicitud, y devuelve las rutas relativas guardadas (para persistir en
     * {@code CambioEstadoSolicitud.archivosAdjuntos}).
     *
     * @throws ArchivoInvalidoException si algún archivo no es de un tipo permitido
     */
    public List<String> guardarTodos(String numeroSolicitud, List<MultipartFile> archivos) {
        return archivos.stream()
                .filter(archivo -> !archivo.isEmpty())
                .map(archivo -> guardar(numeroSolicitud, archivo))
                .toList();
    }

    private String guardar(String numeroSolicitud, MultipartFile archivo) {
        String extension = extensionDe(archivo.getOriginalFilename());
        if (!EXTENSIONES_PERMITIDAS.contains(extension)
                || !CONTENT_TYPES_PERMITIDOS.contains(archivo.getContentType())) {
            throw new ArchivoInvalidoException(
                    "El archivo \"" + archivo.getOriginalFilename() + "\" no es un tipo permitido "
                            + "(solo PDF, JPG o PNG)");
        }

        String nombreSanitizado = sanitizar(archivo.getOriginalFilename());
        String nombreGuardado = UUID.randomUUID() + "_" + nombreSanitizado;
        String rutaRelativa = numeroSolicitud + "/" + nombreGuardado;

        try {
            Path carpetaSolicitud = directorioBase.resolve(numeroSolicitud).normalize();
            // El nombre de la solicitud es un identificador generado por el propio
            // sistema (SOL-000123), pero igual verificamos que no se "escape" del
            // directorio base antes de escribir nada en disco.
            if (!carpetaSolicitud.startsWith(directorioBase)) {
                throw new ArchivoInvalidoException("Número de solicitud inválido");
            }
            Files.createDirectories(carpetaSolicitud);

            Path destino = carpetaSolicitud.resolve(nombreGuardado);
            try (InputStream entrada = archivo.getInputStream()) {
                Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Archivo guardado numeroSolicitud={} archivo={}", numeroSolicitud, nombreGuardado);
            return rutaRelativa;
        } catch (IOException ex) {
            log.error("Error guardando archivo numeroSolicitud={} nombreOriginal={}",
                    numeroSolicitud, archivo.getOriginalFilename(), ex);
            throw new IllegalStateException("No se pudo guardar el archivo adjunto", ex);
        }
    }

    /** Devuelve el archivo en disco a partir de la ruta relativa guardada, para que el admin lo descargue. */
    public Path resolverParaDescarga(String rutaRelativa) {
        Path ruta = directorioBase.resolve(rutaRelativa).normalize();
        if (!ruta.startsWith(directorioBase) || !Files.isRegularFile(ruta)) {
            throw new ArchivoInvalidoException("El archivo solicitado no existe");
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
        // Solo letras, números, puntos y guiones: cualquier otra cosa (espacios,
        // barras, caracteres de control) se reemplaza para que el nombre nunca
        // pueda alterar la ruta de destino ni causar problemas en el filesystem.
        return nombre.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }
}
