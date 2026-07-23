package com.almoby.ruralcuruzu.exception;

/**
 * El email ya pertenece a un usuario existente o a otra solicitud "viva"
 * (pendiente, en revisión o aprobada). Documento 5.3: "que el correo no esté registrado".
 */
public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException() {
        super("Ya existe una cuenta o una solicitud en curso con ese email");
    }
}
