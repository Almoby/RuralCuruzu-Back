package com.almoby.ruralcuruzu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.almoby.ruralcuruzu.exception.ArchivoInvalidoException;

/**
 * Tests unitarios de AlmacenamientoComprobantesService. Igual criterio que
 * AlmacenamientoArchivosServiceTest: se usa un directorio temporal real
 * (@TempDir) en vez de mocks, porque el propio contrato del servicio es
 * escribir/leer en disco (path traversal, tipos permitidos, sanitizado de
 * nombres).
 */
class AlmacenamientoComprobantesServiceTest {

    @TempDir
    Path directorioTemporal;

    private AlmacenamientoComprobantesService service;

    @BeforeEach
    void setUp() {
        service = new AlmacenamientoComprobantesService(directorioTemporal.toString());
    }

    @Test
    void guardar_archivoPermitido_loGuardaBajoUnaSubcarpetaConElIdDelPago() {
        MultipartFile archivo = new MockMultipartFile(
                "comprobante", "comprobante transferencia.pdf", "application/pdf", "contenido".getBytes());

        String ruta = service.guardar("pago-1", archivo);

        assertThat(ruta).startsWith("pago-1/").endsWith("_comprobante_transferencia.pdf");
        assertThat(Files.exists(directorioTemporal.resolve(ruta))).isTrue();
    }

    @Test
    void guardar_imagenPermitida_laGuarda() {
        MultipartFile archivo = new MockMultipartFile(
                "comprobante", "foto.jpg", "image/jpeg", "x".getBytes());

        String ruta = service.guardar("pago-2", archivo);

        assertThat(ruta).startsWith("pago-2/");
        assertThat(Files.exists(directorioTemporal.resolve(ruta))).isTrue();
    }

    @Test
    void guardar_extensionNoPermitida_lanzaExcepcion() {
        MultipartFile exe = new MockMultipartFile(
                "comprobante", "virus.exe", "application/octet-stream", "x".getBytes());

        assertThatThrownBy(() -> service.guardar("pago-3", exe))
                .isInstanceOf(ArchivoInvalidoException.class);
    }

    @Test
    void guardar_contentTypeNoCoincideConLaExtension_lanzaExcepcion() {
        MultipartFile archivo = new MockMultipartFile(
                "comprobante", "comprobante.pdf", "application/octet-stream", "x".getBytes());

        assertThatThrownBy(() -> service.guardar("pago-4", archivo))
                .isInstanceOf(ArchivoInvalidoException.class);
    }

    @Test
    void resolverParaDescarga_archivoExistente_devuelveLaRutaAbsoluta() throws IOException {
        MultipartFile archivo = new MockMultipartFile("comprobante", "doc.pdf", "application/pdf", "x".getBytes());
        String rutaRelativa = service.guardar("pago-5", archivo);

        Path resuelto = service.resolverParaDescarga(rutaRelativa);

        assertThat(Files.isSameFile(resuelto, directorioTemporal.resolve(rutaRelativa))).isTrue();
    }

    @Test
    void resolverParaDescarga_intentoDePathTraversal_lanzaExcepcion() {
        assertThatThrownBy(() -> service.resolverParaDescarga("../../etc/passwd"))
                .isInstanceOf(ArchivoInvalidoException.class);
    }

    @Test
    void resolverParaDescarga_archivoInexistente_lanzaExcepcion() {
        assertThatThrownBy(() -> service.resolverParaDescarga("pago-6/no-existe.pdf"))
                .isInstanceOf(ArchivoInvalidoException.class);
    }
}
