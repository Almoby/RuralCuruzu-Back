package com.almoby.ruralcuruzu.exception;

import com.almoby.ruralcuruzu.enums.CategoriaSocio;

public class ReglaCuotaNoEncontradaException extends RuntimeException {

    public ReglaCuotaNoEncontradaException(CategoriaSocio categoria) {
        super("No hay una regla de cuota configurada para la categoría " + categoria);
    }
}
