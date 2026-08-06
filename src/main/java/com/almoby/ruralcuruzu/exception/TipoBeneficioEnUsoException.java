package com.almoby.ruralcuruzu.exception;

/**
 * Intento de borrado físico de un tipo de beneficio que todavía tiene al
 * menos un Beneficio cargado con ese tipoBeneficioId. Se ofrece desactivarlo
 * en cambio (PATCH activo=false), que no rompe nada de lo ya cargado.
 */
public class TipoBeneficioEnUsoException extends RuntimeException {

    public TipoBeneficioEnUsoException(String id) {
        super("No se puede eliminar el tipo de beneficio " + id
                + ": hay beneficios que lo usan. Podés desactivarlo en cambio.");
    }
}
