package com.almoby.ruralcuruzu.security.jwt;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.almoby.ruralcuruzu.exception.CodigoQrExpiradoException;
import com.almoby.ruralcuruzu.exception.CodigoQrInvalidoException;

import lombok.extern.slf4j.Slf4j;

/**
 * Alias corto y legible para el token JWT de "Mi QR" (mejora pedida por el
 * front: la pantalla de "Validar QR" del comercio tiene un campo para pegar
 * el código a mano si el escaneo con cámara falla, pero el JWT completo
 * ronda los 230 caracteres — inviable para tipear o dictar). Este servicio
 * genera un código tipo "qhdj-jklp-jikl-kjlp" que representa exactamente al
 * mismo token, sin cambiar en nada el flujo de escaneo: el QR visual sigue
 * codificando el JWT largo de siempre, esto es solo una puerta de entrada
 * alternativa para el mismo endpoint de canje.
 *
 * En memoria a propósito (mismo criterio que RateLimiterService y
 * NotificacionSseServiceImpl): es un alias efímero, vive lo mismo que el
 * token real (unos pocos minutos, ver qr.token-validez-segundos) y no hay
 * necesidad de persistirlo ni compartirlo entre instancias.
 */
@Slf4j
@Component
public class CodigoQrCortoService {

    /** Sin 0/O/1/I/L/U: son los caracteres que más se confunden al leer o tipear a mano. */
    private static final String ALFABETO = "23456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int LARGO = 16;
    private static final int LARGO_GRUPO = 4;
    private static final Pattern PATRON_CODIGO_CORTO = Pattern.compile("^[" + ALFABETO + "]{" + LARGO + "}$");

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entrada> tokensPorCodigo = new ConcurrentHashMap<>();

    /** Genera un código corto nuevo que representa a {@code tokenJwt}, válido hasta {@code expiraEn}. */
    public String generar(String tokenJwt, Instant expiraEn) {
        String codigo;
        Entrada entrada = new Entrada(tokenJwt, expiraEn);
        do {
            codigo = codigoAleatorio();
        } while (tokensPorCodigo.putIfAbsent(codigo, entrada) != null);
        // Se guarda en el mapa en mayúsculas (mismo criterio que normalizar(), para
        // que la búsqueda no dependa de mayúsculas/minúsculas), pero se muestra en
        // minúsculas: más cómodo de leer y tipear a mano.
        return formatear(codigo).toLowerCase();
    }

    /**
     * Si {@code valor} tiene la forma de un código corto generado acá, devuelve
     * el JWT real que representa. Si no tiene esa forma —el caso normal del
     * escaneo, que manda el JWT tal cual— lo devuelve sin tocar, para que el
     * resto del flujo de canje siga funcionando exactamente igual que antes.
     *
     * Mismos criterios de error que {@link QrTokenService#extraerSocioId}, para
     * que el comercio vea el mismo tipo de mensaje sin importar si escaneó o
     * pegó el código a mano: {@link CodigoQrInvalidoException} si el código no
     * existe (no fue generado, o ya lo limpió el housekeeping tiempo después
     * de vencer), {@link CodigoQrExpiradoException} si todavía está en memoria
     * pero su ventana de vigencia ya pasó (el caso esperado si el comercio
     * tardó en escribirlo).
     */
    public String resolverOMismo(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = normalizar(valor);
        if (!PATRON_CODIGO_CORTO.matcher(normalizado).matches()) {
            return valor;
        }

        Entrada entrada = tokensPorCodigo.get(normalizado);
        if (entrada == null) {
            log.debug("Código QR corto inexistente: {}", normalizado);
            throw new CodigoQrInvalidoException();
        }
        if (entrada.expiraEn().isBefore(Instant.now())) {
            tokensPorCodigo.remove(normalizado, entrada);
            log.debug("Código QR corto vencido: {}", normalizado);
            throw new CodigoQrExpiradoException();
        }
        return entrada.tokenJwt();
    }

    /** Limpieza periódica para no acumular en memoria códigos de QRs ya vencidos hace rato. */
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    void limpiarCodigosExpirados() {
        Instant ahora = Instant.now();
        int antes = tokensPorCodigo.size();
        tokensPorCodigo.entrySet().removeIf(e -> e.getValue().expiraEn().isBefore(ahora));
        int eliminados = antes - tokensPorCodigo.size();
        if (eliminados > 0) {
            log.debug("Códigos QR cortos: {} vencidos eliminados de memoria", eliminados);
        }
    }

    private String codigoAleatorio() {
        StringBuilder sb = new StringBuilder(LARGO);
        for (int i = 0; i < LARGO; i++) {
            sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    private String normalizar(String valor) {
        return valor.trim().toUpperCase().replace("-", "").replace(" ", "");
    }

    private String formatear(String codigoPlano) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < codigoPlano.length(); i++) {
            if (i > 0 && i % LARGO_GRUPO == 0) {
                sb.append('-');
            }
            sb.append(codigoPlano.charAt(i));
        }
        return sb.toString();
    }

    private record Entrada(String tokenJwt, Instant expiraEn) {
    }
}
