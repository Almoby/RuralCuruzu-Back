package com.almoby.ruralcuruzu.service.impl;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.domain.HistorialBeneficio;
import com.almoby.ruralcuruzu.dto.response.ConsumoRecienteResponse;
import com.almoby.ruralcuruzu.dto.response.EstadisticasComercioResponse;
import com.almoby.ruralcuruzu.dto.response.IndicadoresComercioResponse;
import com.almoby.ruralcuruzu.dto.response.IndicadoresEstadisticasComercioResponse;
import com.almoby.ruralcuruzu.dto.response.InicioComercioResponse;
import com.almoby.ruralcuruzu.dto.response.UsoDiaSemanaResponse;
import com.almoby.ruralcuruzu.dto.response.UsoMensualResponse;
import com.almoby.ruralcuruzu.dto.response.UsoPorPromocionResponse;
import com.almoby.ruralcuruzu.enums.EstadoUsoBeneficio;
import com.almoby.ruralcuruzu.repository.BeneficioRepository;
import com.almoby.ruralcuruzu.repository.HistorialBeneficioRepository;
import com.almoby.ruralcuruzu.service.ComercioDashboardService;
import com.almoby.ruralcuruzu.util.FechaUtil;

/**
 * "Inicio" y "Estadísticas" del portal de comercio (equivalente reducido,
 * para el propio comercio, del dashboard admin de la sección 7). Todo de
 * solo lectura, sobre los mismos datos que ya usa el panel admin
 * ({@link HistorialBeneficio} en estado USADO), filtrados por comercioId.
 *
 * <p>Los indicadores ("este mes") y el gráfico ("esta semana") de Inicio se resuelven
 * con una única consulta a {@code HistorialBeneficio} en vez de dos: se pide
 * todo desde la fecha más antigua entre "inicio del mes" e "inicio de la
 * semana" (el inicio de semana puede caer en el mes anterior) y después se
 * filtra en memoria para cada rango. Solo "socios alcanzados" (histórico
 * completo) y "promociones activas" quedan como consultas aparte, porque
 * cubren un rango distinto (todo el tiempo) o una colección distinta.
 */
@Service
public class ComercioDashboardServiceImpl implements ComercioDashboardService {

    /** Cuántas filas mostrar en "Detalle de consumos recientes". */
    private static final int CANTIDAD_CONSUMOS_RECIENTES = 10;

    private final BeneficioRepository beneficioRepository;
    private final HistorialBeneficioRepository historialBeneficioRepository;

    public ComercioDashboardServiceImpl(BeneficioRepository beneficioRepository,
                                         HistorialBeneficioRepository historialBeneficioRepository) {
        this.beneficioRepository = beneficioRepository;
        this.historialBeneficioRepository = historialBeneficioRepository;
    }

    @Override
    public InicioComercioResponse obtenerInicio(String comercioId) {
        Instant inicioMes = FechaUtil.inicioDeMesActual();
        Instant inicioSemana = FechaUtil.inicioDeSemanaActual();
        Instant desde = inicioSemana.isBefore(inicioMes) ? inicioSemana : inicioMes;

        List<HistorialBeneficio> usosRecientes = usosValidosDesde(comercioId, desde);
        List<HistorialBeneficio> usosDelMes = usosRecientes.stream()
                .filter(h -> !h.getFechaUso().isBefore(inicioMes))
                .toList();
        List<HistorialBeneficio> usosDeLaSemana = usosRecientes.stream()
                .filter(h -> !h.getFechaUso().isBefore(inicioSemana))
                .toList();

        return new InicioComercioResponse(
                calcularIndicadores(comercioId, usosDelMes),
                agruparPorDia(usosDeLaSemana));
    }

    @Override
    public EstadisticasComercioResponse obtenerEstadisticas(String comercioId, int anio) {
        // Una sola consulta al histórico completo del comercio: de acá salen el total
        // histórico, los socios únicos, los usos de este mes, la serie mensual del año
        // pedido, el uso por promoción de este mes y el detalle de consumos recientes.
        List<HistorialBeneficio> historial = historialBeneficioRepository.findByComercioId(comercioId).stream()
                .filter(h -> h.getEstado() == EstadoUsoBeneficio.USADO)
                .toList();

        Instant inicioMesActual = FechaUtil.inicioDeMesActual();
        List<HistorialBeneficio> usosDelMes = historial.stream()
                .filter(h -> h.getFechaUso() != null && !h.getFechaUso().isBefore(inicioMesActual))
                .toList();

        long promocionesActivas = beneficioRepository.findByComercioId(comercioId).stream()
                .filter(Beneficio::estaVigenteHoy)
                .count();
        long sociosUnicos = historial.stream().map(HistorialBeneficio::getSocioId).distinct().count();

        IndicadoresEstadisticasComercioResponse indicadores = new IndicadoresEstadisticasComercioResponse(
                historial.size(), sociosUnicos, promocionesActivas, usosDelMes.size());

        return new EstadisticasComercioResponse(
                indicadores,
                usosMensuales(historial, anio),
                usosPorPromocion(usosDelMes),
                consumosRecientes(historial));
    }

    private List<UsoMensualResponse> usosMensuales(List<HistorialBeneficio> historial, int anio) {
        Map<String, Long> cantidadPorPeriodo = historial.stream()
                .filter(h -> h.getFechaUso() != null)
                .collect(Collectors.groupingBy(
                        h -> YearMonth.from(h.getFechaUso().atZone(ZoneId.systemDefault())).toString(),
                        Collectors.counting()));

        List<UsoMensualResponse> resultado = new ArrayList<>();
        for (int mes = 1; mes <= 12; mes++) {
            String periodo = YearMonth.of(anio, mes).toString();
            resultado.add(new UsoMensualResponse(
                    periodo, FechaUtil.NOMBRES_MES[mes - 1], cantidadPorPeriodo.getOrDefault(periodo, 0L)));
        }
        return resultado;
    }

    private List<UsoPorPromocionResponse> usosPorPromocion(List<HistorialBeneficio> usosDelMes) {
        Map<String, List<HistorialBeneficio>> porBeneficio = usosDelMes.stream()
                .collect(Collectors.groupingBy(HistorialBeneficio::getBeneficioId));

        return porBeneficio.entrySet().stream()
                .map(entry -> new UsoPorPromocionResponse(
                        entry.getKey(), entry.getValue().get(0).getBeneficioTitulo(), entry.getValue().size()))
                .sorted(Comparator.comparingLong(UsoPorPromocionResponse::cantidad).reversed())
                .toList();
    }

    private List<ConsumoRecienteResponse> consumosRecientes(List<HistorialBeneficio> historial) {
        return historial.stream()
                .sorted(Comparator.comparing(HistorialBeneficio::getFechaUso,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(CANTIDAD_CONSUMOS_RECIENTES)
                .map(h -> new ConsumoRecienteResponse(h.getSocioNombre(), h.getBeneficioTitulo(), h.getFechaUso()))
                .toList();
    }

    private IndicadoresComercioResponse calcularIndicadores(String comercioId, List<HistorialBeneficio> usosDelMes) {
        long promocionesActivas = beneficioRepository.findByComercioId(comercioId).stream()
                .filter(Beneficio::estaVigenteHoy)
                .count();

        Instant inicioDeHoy = FechaUtil.inicioDeHoy();
        long validacionesHoy = usosDelMes.stream()
                .filter(h -> !h.getFechaUso().isBefore(inicioDeHoy))
                .count();

        // Histórico completo (no solo este mes): "alcance" real del comercio entre los socios.
        long sociosAlcanzados = historialBeneficioRepository.findByComercioId(comercioId).stream()
                .filter(h -> h.getEstado() == EstadoUsoBeneficio.USADO)
                .map(HistorialBeneficio::getSocioId)
                .distinct()
                .count();

        return new IndicadoresComercioResponse(usosDelMes.size(), promocionesActivas, sociosAlcanzados, validacionesHoy);
    }

    private List<UsoDiaSemanaResponse> agruparPorDia(List<HistorialBeneficio> usosDeLaSemana) {
        Map<DayOfWeek, Long> usosPorDia = usosDeLaSemana.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getFechaUso().atZone(ZoneId.systemDefault()).getDayOfWeek(),
                        Collectors.counting()));

        return Arrays.stream(DayOfWeek.values())
                .map(dia -> new UsoDiaSemanaResponse(dia, usosPorDia.getOrDefault(dia, 0L)))
                .toList();
    }

    private List<HistorialBeneficio> usosValidosDesde(String comercioId, Instant desde) {
        return historialBeneficioRepository.findByComercioIdAndFechaUsoAfter(comercioId, desde).stream()
                .filter(h -> h.getEstado() == EstadoUsoBeneficio.USADO)
                .toList();
    }
}
