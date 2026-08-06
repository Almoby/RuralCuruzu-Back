package com.almoby.ruralcuruzu.exception;

/**
 * El comercio mandó un tipoBeneficioId al crear/editar un Beneficio que no
 * existe en el catálogo, o que existe pero está desactivado (solo los
 * activos son elegibles para beneficios nuevos).
 */
public class TipoBeneficioInvalidoException extends RuntimeException {

    public TipoBeneficioInvalidoException(String tipoBeneficioId) {
        super("El tipo de beneficio " + tipoBeneficioId + " no existe o no está activo");
    }
}
