package com.almoby.ruralcuruzu.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;

/**
 * Pin de la lógica compartida entre CuotaSocioController y
 * SolicitudSocioAdminController antes de extraerla (Strict TDD): ambos
 * controllers tenían copias idénticas de estos dos métodos.
 */
class ArchivoDescargaUtilTest {

    @TempDir
    Path directorioTemporal;

    @Test
    void nombreParaDescarga_conPrefijoUuid_loSaca() {
        Path archivo = directorioTemporal.resolve("550e8400-e29b-41d4-a716-446655440000_comprobante.pdf");

        String nombre = ArchivoDescargaUtil.nombreParaDescarga(archivo);

        assertThat(nombre).isEqualTo("comprobante.pdf");
    }

    @Test
    void nombreParaDescarga_sinPrefijoUuid_loDejaSinCambios() {
        Path archivo = directorioTemporal.resolve("comprobante.pdf");

        String nombre = ArchivoDescargaUtil.nombreParaDescarga(archivo);

        assertThat(nombre).isEqualTo("comprobante.pdf");
    }

    @Test
    void tipoDeContenido_extensionConTipoResoluble_devuelveElTipoProbado() throws IOException {
        Path archivo = directorioTemporal.resolve("documento.txt");
        Files.writeString(archivo, "contenido");

        MediaType contentType = ArchivoDescargaUtil.tipoDeContenido(archivo);

        String tipoEsperado = Files.probeContentType(archivo);
        assertThat(tipoEsperado).isNotNull();
        assertThat(contentType).isEqualTo(MediaType.parseMediaType(tipoEsperado));
    }

    @Test
    void tipoDeContenido_extensionNoResoluble_devuelveOctetStream() throws IOException {
        Path archivo = directorioTemporal.resolve("archivo.extensiondesconocida123");
        Files.writeString(archivo, "contenido");

        MediaType contentType = ArchivoDescargaUtil.tipoDeContenido(archivo);

        assertThat(contentType).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    void tipoDeContenido_probeContentTypeLanzaIOException_devuelveOctetStream() {
        Path archivo = directorioTemporal.resolve("comprobante.pdf");

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.probeContentType(archivo)).thenThrow(new IOException("boom"));

            MediaType contentType = ArchivoDescargaUtil.tipoDeContenido(archivo);

            assertThat(contentType).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        }
    }
}
