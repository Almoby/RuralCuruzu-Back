package com.almoby.ruralcuruzu.exception;

/**
 * El DNI, el CUIL (persona física) o el CUIT (persona jurídica) ya corresponde
 * a otra solicitud "viva". Documento 5.3: "que el DNI o CUIT no corresponda a otro socio".
 */
public class DocumentoYaRegistradoException extends RuntimeException {

    public DocumentoYaRegistradoException() {
        super("Ya existe una solicitud en curso con ese DNI/CUIL/CUIT");
    }
}
