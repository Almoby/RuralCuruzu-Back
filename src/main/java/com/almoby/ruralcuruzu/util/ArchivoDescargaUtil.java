package com.almoby.ruralcuruzu.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.http.MediaType;

/**
 * Utilidades compartidas para armar la respuesta de descarga de un archivo
 * (comprobantes de pago, adjuntos de una solicitud de socio). Evita repetir
 * la misma lógica en cada controller que expone un endpoint de descarga.
 */
public final class ArchivoDescargaUtil {

    private ArchivoDescargaUtil() {
    }

    /** Le saca el prefijo UUID_ con el que se guardó el archivo, para que el usuario vea el nombre original. */
    public static String nombreParaDescarga(Path archivo) {
        return archivo.getFileName().toString().replaceFirst(
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_", "");
    }

    /** Content-type del archivo según su extensión, o application/octet-stream si no se puede determinar. */
    public static MediaType tipoDeContenido(Path archivo) {
        try {
            String contentType = Files.probeContentType(archivo);
            return contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (IOException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
