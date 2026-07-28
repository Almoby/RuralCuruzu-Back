package com.almoby.ruralcuruzu.exception;

/** Se lanza cuando todavía ningún admin cargó los datos bancarios de la cooperativa. */
public class DatosBancariosNoConfiguradosException extends RuntimeException {

    public DatosBancariosNoConfiguradosException() {
        super("Los datos bancarios todavía no fueron configurados");
    }
}
