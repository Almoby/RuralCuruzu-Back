package com.almoby.ruralcuruzu.enums;

import java.util.Set;

/**
 * Con qué frecuencia paga la cuota un socio (pedido del dueño del proyecto:
 * "hay socios que pagan de manera anual, cuota semestral, etc.", no todos
 * mes a mes). Es un dato del Socio, independiente de su categoría
 * (ACTIVO/ADHERENTE, que solo define el importe base): dos socios de la
 * misma categoría pueden tener periodicidades distintas.
 *
 * <p>La generación mensual de cuotas (CuotaServiceImpl.generarCuotas) usa
 * {@link #mesesDeGeneracion()} para decidir en qué meses del año le toca a
 * cada socio, y {@link #cantidadDeMeses()} para multiplicar el importe
 * mensual de su categoría (una cuota trimestral cuesta 3 veces la mensual,
 * etc.). Por defecto todo socio queda en MENSUAL (comportamiento histórico,
 * sin este campo).
 */
public enum PeriodicidadPago {

    MENSUAL,
    TRIMESTRAL,
    SEMESTRAL,
    ANUAL;

    /** Meses del año (1-12) en los que corresponde generarle una cuota a un socio con esta periodicidad. */
    public Set<Integer> mesesDeGeneracion() {
        return switch (this) {
            case MENSUAL -> Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
            case TRIMESTRAL -> Set.of(1, 4, 7, 10);
            case SEMESTRAL -> Set.of(1, 7);
            case ANUAL -> Set.of(1);
        };
    }

    /** Cuántos meses cubre cada cuota generada con esta periodicidad (multiplica el importe mensual de la categoría). */
    public int cantidadDeMeses() {
        return switch (this) {
            case MENSUAL -> 1;
            case TRIMESTRAL -> 3;
            case SEMESTRAL -> 6;
            case ANUAL -> 12;
        };
    }
}
