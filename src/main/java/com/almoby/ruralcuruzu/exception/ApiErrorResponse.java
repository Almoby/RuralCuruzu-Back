package com.almoby.ruralcuruzu.exception;

import java.time.Instant;
import java.util.List;

/**
 * Formato único de error devuelto por toda la API.
 * {@code errores} solo se completa en errores de validación (400 por @Valid);
 * en el resto de los casos queda null. {@code codigo} es un código estable
 * y opcional (ej. el motivo de un QR rechazado) para los pocos casos donde el
 * frontend necesita distinguir el error sin parsear {@code message}; el resto
 * de las excepciones lo deja null.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<CampoError> errores,
        String codigo
) {

    public record CampoError(String campo, String mensaje) {
    }

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, null, null);
    }

    public static ApiErrorResponse ofValidacion(int status, String error, String message, String path,
                                                 List<CampoError> errores) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, errores, null);
    }

    public static ApiErrorResponse conCodigo(int status, String error, String message, String path, String codigo) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, null, codigo);
    }
}
