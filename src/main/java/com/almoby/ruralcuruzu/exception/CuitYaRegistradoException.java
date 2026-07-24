package com.almoby.ruralcuruzu.exception;

/**
 * Ya existe un Comercio registrado con ese CUIT (documento, sección 12.2).
 * A diferencia de SolicitudSocio, acá el CUIT identifica al comercio "de
 * verdad" (no hay reintentos tipo solicitud): mientras el comercio exista,
 * sin importar su estado, ese CUIT queda bloqueado para un alta nueva.
 */
public class CuitYaRegistradoException extends RuntimeException {

    public CuitYaRegistradoException() {
        super("Ya existe un comercio registrado con ese CUIT");
    }
}
