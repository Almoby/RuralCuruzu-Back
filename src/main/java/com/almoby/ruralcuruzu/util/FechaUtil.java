package com.almoby.ruralcuruzu.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utilidades de fecha compartidas entre servicios que calculan indicadores
 * "de este mes"/"de esta semana"/"de hoy" (dashboard admin, dashboard de
 * comercio, listado de beneficios propios). Evita repetir la misma cuenta en
 * cada service.
 */
public final class FechaUtil {

    /** Nombres cortos de mes (índice 0 = enero), para las series mensuales de los dashboards. */
    public static final String[] NOMBRES_MES = {
            "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    };

    /** Formato corto de fecha ("dd/MM/yyyy") compartido por los encabezados de los PDF generados. */
    public static final DateTimeFormatter FORMATO_FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FechaUtil() {
    }

    /** Medianoche del día 1 del mes actual, en la zona horaria del servidor. */
    public static Instant inicioDeMesActual() {
        return YearMonth.now().atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    /** Medianoche del lunes de la semana actual (semana ISO), en la zona horaria del servidor. */
    public static Instant inicioDeSemanaActual() {
        return LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    /** Medianoche de hoy, en la zona horaria del servidor. */
    public static Instant inicioDeHoy() {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
