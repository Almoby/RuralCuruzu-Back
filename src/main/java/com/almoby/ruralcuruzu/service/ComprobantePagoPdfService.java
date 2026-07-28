package com.almoby.ruralcuruzu.service;

import com.almoby.ruralcuruzu.dto.response.PagoResponse;

/**
 * Genera una constancia de pago en PDF al vuelo, para los {@code Pago} que no
 * tienen un archivo real de comprobante (registrados por un admin en
 * ventanilla, o pagados por Mercado Pago): a diferencia de una transferencia
 * informada por el socio, ahí nunca hubo un archivo que el socio haya subido,
 * pero igual necesita algo para descargar/imprimir como respaldo del pago.
 */
public interface ComprobantePagoPdfService {

    /** Solo tiene sentido para un pago ya APROBADO; no valida el estado, eso es responsabilidad de quien la llama. */
    byte[] generarConstancia(PagoResponse pago);
}
