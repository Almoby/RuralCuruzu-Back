package com.almoby.ruralcuruzu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.almoby.ruralcuruzu.exception.ArchivoInvalidoException;

/**
 * Tests unitarios de AlmacenamientoArchivosService. A diferencia del resto de
 * los tests de service, acá se usa un directorio temporal real (@TempDir) en
 * vez de mocks: el propio contrato del servicio es escribir/leer en disco, así
 * que mockear el filesystem no probaría nada útil (path traversal, sanitizado
 * de nombres, extensiones permitidas).
 */
class AlmacenamientoArchivosServiceTest {

    @TempDir
    Path directorioTemporal;

    private AlmacenamientoArchivosService service;

    @BeforeEach
    void setUp() {
        service = new AlmacenamientoArchivosService(directorioTemporal.toString());
    }

    @Test
    void guardarTodos_archivoPermitido_loGuardaYDevuelveLaRutaRelativa() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "comprobante domicilio.pdf", "application/pdf", "contenido".getBytes());

        List<String> rutas = service.guardarTodos("SOL-000001", List.of(archivo));

        assertThat(rutas).hasSize(1);
        String ruta = rutas.get(0);
        assertThat(ruta).startsWith("SOL-000001/").endsWith("_comprobante_domicilio.pdf");
        assertThat(Files.exists(directorioTemporal.resolve(ruta))).isTrue();
    }

    @Test
    void guardarTodos_variosArchivosPermitidos_guardaTodosBajoLaMismaCarpeta() {
        MultipartFile pdf = new MockMultipartFile("a", "doc.pdf", "application/pdf", "x".getBytes());
        MultipartFile jpg = new MockMultipartFile("b", "foto.jpg", "image/jpeg", "y".getBytes());

        List<String> rutas = service.guardarTodos("SOL-000002", List.of(pdf, jpg));

        assertThat(rutas).hasSize(2);
        assertThat(rutas).allMatch(r -> r.startsWith("SOL-000002/"));
    }

    @Test
    void guardarTodos_extensionNoPermitida_lanzaExcepcionYNoGuardaNada() {
        MultipartFile exe = new MockMultipartFile("archivo", "virus.exe", "application/octet-stream", "x".getBytes());

        assertThatThrownBy(() -> service.guardarTodos("SOL-000003", List.of(exe)))
                .isInstanceOf(ArchivoInvalidoException.class);
    }

    @Test
    void guardarTodos_contentTypeNoCoincideConLaExtension_lanzaExcepcion() {
        // Extensión permitida (.pdf) pero content-type mentiroso: igual se rechaza.
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "documento.pdf", "application/octet-stream", "x".getBytes());

        assertThatThrownBy(() -> service.guardarTodos("SOL-000004", List.of(archivo)))
                .isInstanceOf(ArchivoInvalidoException.class);
    }

    @Test
    void guardarTodos_archivoVacio_seOmite() {
        MultipartFile vacio = new MockMultipartFile("archivo", "vacio.pdf", "application/pdf", new byte[0]);

        List<String> rutas = service.guardarTodos("SOL-000005", List.of(vacio));

        assertThat(rutas).isEmpty();
    }

    @Test
    void resolverParaDescarga_archivoExistente_devuelveLaRutaAbsoluta() throws IOException {
        MultipartFile archivo = new MockMultipartFile("archivo", "doc.pdf", "application/pdf", "x".getBytes());
        String rutaRelativa = service.guardarTodos("SOL-000006", List.of(archivo)).get(0);

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
        assertThatThrownBy(() -> service.resolverParaDescarga("SOL-000007/no-existe.pdf"))
                .isInstanceOf(ArchivoInvalidoException.class);
    }
}
