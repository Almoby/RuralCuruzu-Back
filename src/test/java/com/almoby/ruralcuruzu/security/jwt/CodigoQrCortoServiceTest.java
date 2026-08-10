package com.almoby.ruralcuruzu.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.almoby.ruralcuruzu.exception.CodigoQrExpiradoException;
import com.almoby.ruralcuruzu.exception.CodigoQrInvalidoException;

/**
 * Alias corto para el JWT del QR (documento: campo "Pegá el código del QR"
 * en la pantalla de Validar QR del comercio, para cuando el escaneo falla).
 * Es un servicio en memoria simple, sin dependencias externas, así que estos
 * tests lo instancian directo (sin mocks).
 */
class CodigoQrCortoServiceTest {

    private final CodigoQrCortoService service = new CodigoQrCortoService();

    @Test
    void generar_devuelveUnCodigoConElFormatoEsperado() {
        String codigo = service.generar("jwt-de-ejemplo", Instant.now().plusSeconds(300));

        // En minúsculas: más cómodo de leer/tipear a mano que un código en mayúsculas.
        assertThat(codigo).matches("^[23456789abcdefghjkmnpqrstvwxyz]{4}(-[23456789abcdefghjkmnpqrstvwxyz]{4}){3}$");
    }

    @Test
    void generar_dosLlamadas_devuelvenCodigosDistintos() {
        String primero = service.generar("jwt-1", Instant.now().plusSeconds(300));
        String segundo = service.generar("jwt-2", Instant.now().plusSeconds(300));

        assertThat(primero).isNotEqualTo(segundo);
    }

    @Test
    void resolverOMismo_conUnJwtNormal_loDevuelveSinTocar() {
        // Cualquier string que no tenga la forma exacta de un código corto (16
        // caracteres del alfabeto reducido) se asume que es el JWT del escaneo.
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzb2Npby0xIn0.firma-de-mentira";

        String resuelto = service.resolverOMismo(jwt);

        assertThat(resuelto).isEqualTo(jwt);
    }

    @Test
    void resolverOMismo_conElCodigoGenerado_devuelveElJwtOriginal() {
        String codigo = service.generar("jwt-real", Instant.now().plusSeconds(300));

        String resuelto = service.resolverOMismo(codigo);

        assertThat(resuelto).isEqualTo("jwt-real");
    }

    @Test
    void resolverOMismo_esInsensibleAMayusculasEspaciosYGuiones() {
        // generar() devuelve el código en minúsculas, pero el comercio lo puede
        // pegar en mayúsculas (o con espacios en vez de guiones, o con espacios
        // de más) y tiene que resolver igual.
        String codigo = service.generar("jwt-real", Instant.now().plusSeconds(300));
        String pegadoDesprolijo = "  " + codigo.toUpperCase().replace("-", " ") + "  ";

        String resuelto = service.resolverOMismo(pegadoDesprolijo);

        assertThat(resuelto).isEqualTo("jwt-real");
    }

    @Test
    void resolverOMismo_codigoYaVencido_lanzaCodigoQrExpiradoException() {
        // Mismo criterio que QrTokenService.extraerSocioId con un JWT vencido:
        // "vencido" (existió, pero se pasó de tiempo) es un caso distinto de
        // "inexistente", y el comercio debería ver el mismo mensaje accionable
        // sin importar si escaneó o pegó el código a mano.
        String codigo = service.generar("jwt-real", Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> service.resolverOMismo(codigo))
                .isInstanceOf(CodigoQrExpiradoException.class);
    }

    @Test
    void resolverOMismo_codigoQueNuncaExistio_lanzaCodigoQrInvalidoException() {
        assertThatThrownBy(() -> service.resolverOMismo("YYYY-YYYY-YYYY-YYYY"))
                .isInstanceOf(CodigoQrInvalidoException.class);
    }

    @Test
    void resolverOMismo_conNull_devuelveNull() {
        assertThat(service.resolverOMismo(null)).isNull();
    }

    @Test
    void resolverOMismo_conStringVacio_loDevuelveSinTocar() {
        assertThat(service.resolverOMismo("")).isEmpty();
    }
}
