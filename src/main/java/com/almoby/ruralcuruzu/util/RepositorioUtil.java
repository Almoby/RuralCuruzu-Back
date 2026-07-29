package com.almoby.ruralcuruzu.util;

import java.util.Optional;
import java.util.function.Function;

/**
 * Utilidad compartida para el patrón "buscar por id o lanzar una excepción de
 * dominio", aplicada únicamente a los sitios cuya forma es exactamente esa:
 * un finder por id y una excepción que toma el id buscado. No reemplaza
 * lookups con formas distintas (ownership checks, singletons sin id, lookups
 * con efectos secundarios).
 */
public final class RepositorioUtil {

    private RepositorioUtil() {
    }

    /**
     * Busca con {@code buscador} usando {@code id}; si no encuentra nada,
     * lanza la excepción construida por {@code excepcion} con ese mismo id.
     * Pasar el mismo {@code id} a ambas funciones hace que "la excepción
     * reporta el id por el que se buscó" sea estructural, no una convención
     * que se pueda romper por accidente.
     */
    public static <T> T buscarOFallar(Function<String, Optional<T>> buscador,
                                       String id,
                                       Function<String, ? extends RuntimeException> excepcion) {
        return buscador.apply(id).orElseThrow(() -> excepcion.apply(id));
    }
}
