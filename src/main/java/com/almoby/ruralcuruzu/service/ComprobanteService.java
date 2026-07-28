package com.almoby.ruralcuruzu.service;

import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.almoby.ruralcuruzu.domain.Comprobante;
import com.almoby.ruralcuruzu.domain.Pago;
import com.almoby.ruralcuruzu.dto.response.PagoResponse;

/**
 * El comprobante de un pago como su propia entidad (documento, sección 10.4;
 * ver {@link Comprobante}). No confundir con {@link AlmacenamientoComprobantesService},
 * que solo se encarga de leer/escribir el archivo en disco: esta capa decide
 * cuándo corresponde generar una constancia y persiste los metadatos.
 */
public interface ComprobanteService {

    /**
     * Registra el comprobante de un pago recién informado por transferencia, justo
     * después de guardar en disco el archivo que adjuntó el socio (CuotaService.informarPago).
     */
    Comprobante registrarSubidoPorSocio(Pago pago, String ruta, MultipartFile archivoOriginal);

    /**
     * Devuelve el comprobante descargable de un pago. Si ya existe (subido por el
     * socio, o generado antes), lo devuelve tal cual. Si no existe pero el pago
     * tiene el dato legacy {@code Pago.comprobanteRuta} (de antes de que Comprobante
     * existiera como entidad propia), lo migra de forma perezosa la primera vez que
     * se pide. Si tampoco hay nada de eso y el pago ya está APROBADO, genera y
     * persiste una constancia en PDF (una sola vez; queda cacheada para las próximas
     * descargas). Vacío si el pago no tiene comprobante y tampoco admite generar uno
     * todavía (no está aprobado).
     */
    Optional<Comprobante> obtenerOGenerarParaPago(PagoResponse pago, String socioId);
}
