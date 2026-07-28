package com.almoby.ruralcuruzu.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.domain.Cuota;
import com.almoby.ruralcuruzu.domain.HistorialBeneficio;
import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.dto.response.BeneficioMasUtilizadoResponse;
import com.almoby.ruralcuruzu.dto.response.CobranzaMensualResponse;
import com.almoby.ruralcuruzu.dto.response.DashboardPrincipalResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoSociosResponse;
import com.almoby.ruralcuruzu.dto.response.IndicadoresPrincipalesResponse;
import com.almoby.ruralcuruzu.dto.response.UsoBeneficioPorComercioResponse;
import com.almoby.ruralcuruzu.dto.response.UsoPeriodoResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoBeneficio;
import com.almoby.ruralcuruzu.enums.EstadoComercio;
import com.almoby.ruralcuruzu.enums.EstadoCuota;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.EstadoUsoBeneficio;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.repository.BeneficioRepository;
import com.almoby.ruralcuruzu.repository.ComercioRepository;
import com.almoby.ruralcuruzu.repository.CuotaRepository;
import com.almoby.ruralcuruzu.repository.HistorialBeneficioRepository;
import com.almoby.ruralcuruzu.repository.SocioRepository;
import com.almoby.ruralcuruzu.service.DashboardService;
import com.almoby.ruralcuruzu.util.FechaUtil;

/**
 * Ver documento, sección 7. Todo de solo lectura, agregando sobre las
 * colecciones de Socio/Cuota/Comercio/HistorialBeneficio ya existentes: el
 * dashboard no tiene colección propia.
 *
 * Reglas acordadas con el usuario (no están en el documento, se definieron
 * para esta implementación):
 * - "Facturación mensual" = suma de TODAS las cuotas del período actual
 *   (facturado), excepto las ANULADAS, sin importar si ya se pagaron.
 * - "Deuda acumulada" = suma de las cuotas actualmente en estado VENCIDA
 *   únicamente.
 * - "Beneficios utilizados" = usos del mes actual (el acumulado histórico
 *   completo va aparte, en beneficiosUtilizadosHistoricoTotal).
 * - Clasificación al día/pendiente/vencido de un socio: solo se calcula
 *   sobre socios con EstadoSocio ACTIVO. Vencido si tiene alguna cuota
 *   VENCIDA; si no, pendiente si tiene alguna PENDIENTE/INFORMADA/
 *   EN_REVISION; si no, al día (incluye no tener cuotas todavía). RECHAZADA
 *   y ANULADA no se consideran deuda, igual que ya hace CuotaServiceImpl.
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private final SocioRepository socioRepository;
    private final CuotaRepository cuotaRepository;
    private final ComercioRepository comercioRepository;
    private final BeneficioRepository beneficioRepository;
    private final HistorialBeneficioRepository historialBeneficioRepository;

    public DashboardServiceImpl(SocioRepository socioRepository,
                                 CuotaRepository cuotaRepository,
                                 ComercioRepository comercioRepository,
                                 BeneficioRepository beneficioRepository,
                                 HistorialBeneficioRepository historialBeneficioRepository) {
        this.socioRepository = socioRepository;
        this.cuotaRepository = cuotaRepository;
        this.comercioRepository = comercioRepository;
        this.beneficioRepository = beneficioRepository;
        this.historialBeneficioRepository = historialBeneficioRepository;
    }

    @Override
    public DashboardPrincipalResponse obtenerDashboardPrincipal(int anio, CategoriaSocio categoria, TipoPersona tipoPersona) {
        return new DashboardPrincipalResponse(
                obtenerIndicadoresPrincipales(),
                obtenerCobranzaMensual(anio),
                obtenerEstadoSocios(categoria, tipoPersona),
                obtenerUsoBeneficiosPorComercio(),
                obtenerBeneficiosMasUtilizados());
    }

    @Override
    public IndicadoresPrincipalesResponse obtenerIndicadoresPrincipales() {
        List<Socio> socios = socioRepository.findAll();
        List<Cuota> cuotas = cuotaRepository.findAll();
        Map<String, List<Cuota>> cuotasPorSocio = cuotas.stream().collect(Collectors.groupingBy(Cuota::getSocioId));

        Instant inicioMesActual = FechaUtil.inicioDeMesActual();

        long totalSocios = socios.size();
        long sociosNuevosEsteMes = socios.stream()
                .filter(s -> s.getFechaAlta() != null && !s.getFechaAlta().isBefore(inicioMesActual))
                .count();

        long alDia = 0;
        long pendientes = 0;
        long vencidos = 0;
        for (Socio socio : socios) {
            if (socio.getEstado() != EstadoSocio.ACTIVO) {
                continue;
            }
            switch (clasificar(cuotasPorSocio.getOrDefault(socio.getId(), List.of()))) {
                case VENCIDO -> vencidos++;
                case PENDIENTE -> pendientes++;
                case AL_DIA -> alDia++;
            }
        }
        double porcentajeAlDia = totalSocios == 0 ? 0.0 : (alDia * 100.0 / totalSocios);

        long comerciosActivos = comercioRepository.findByEstado(EstadoComercio.ACTIVO).size();
        // No alcanza con el estado crudo: aunque el job diario (BeneficioServiceImpl.marcarBeneficiosVencidos)
        // ya lo pasa a INACTIVO al otro día, estaVigenteHoy() también chequea el rango de fechas para no
        // depender de que ese job ya haya corrido hoy.
        long promocionesActivas = beneficioRepository.findByEstado(EstadoBeneficio.ACTIVO).stream()
                .filter(Beneficio::estaVigenteHoy)
                .count();

        String periodoActual = YearMonth.now().toString();
        String periodoAnterior = YearMonth.now().minusMonths(1).toString();
        BigDecimal facturacionMensual = sumaImporteFacturado(cuotas, periodoActual);
        BigDecimal facturacionMesAnterior = sumaImporteFacturado(cuotas, periodoAnterior);
        BigDecimal variacionPorcentual = facturacionMesAnterior.compareTo(BigDecimal.ZERO) == 0
                ? null
                : facturacionMensual.subtract(facturacionMesAnterior)
                        .divide(facturacionMesAnterior, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        BigDecimal deudaAcumulada = cuotas.stream()
                .filter(c -> c.getEstado() == EstadoCuota.VENCIDA)
                .map(Cuota::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long sociosEnMora = cuotas.stream()
                .filter(c -> c.getEstado() == EstadoCuota.VENCIDA)
                .map(Cuota::getSocioId)
                .distinct()
                .count();

        List<HistorialBeneficio> historiales = historialBeneficioRepository.findAll();
        long beneficiosUtilizadosEsteMes = historiales.stream()
                .filter(h -> h.getEstado() == EstadoUsoBeneficio.USADO
                        && h.getFechaUso() != null
                        && !h.getFechaUso().isBefore(inicioMesActual))
                .count();
        long beneficiosUtilizadosHistoricoTotal = historiales.stream()
                .filter(h -> h.getEstado() == EstadoUsoBeneficio.USADO)
                .count();

        return new IndicadoresPrincipalesResponse(
                totalSocios, sociosNuevosEsteMes,
                alDia, porcentajeAlDia,
                pendientes,
                vencidos,
                comerciosActivos, promocionesActivas,
                facturacionMensual, variacionPorcentual,
                deudaAcumulada, sociosEnMora,
                beneficiosUtilizadosEsteMes, beneficiosUtilizadosHistoricoTotal);
    }

    @Override
    public List<CobranzaMensualResponse> obtenerCobranzaMensual(int anio) {
        Map<String, List<Cuota>> cuotasPorPeriodo = cuotaRepository.findAll().stream()
                .collect(Collectors.groupingBy(Cuota::getPeriodo));

        List<CobranzaMensualResponse> resultado = new ArrayList<>();
        for (int mes = 1; mes <= 12; mes++) {
            String periodo = YearMonth.of(anio, mes).toString();
            List<Cuota> delMes = cuotasPorPeriodo.getOrDefault(periodo, List.of());

            BigDecimal cobrado = delMes.stream()
                    .filter(c -> c.getEstado() == EstadoCuota.PAGADA)
                    .map(Cuota::getImporte)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal pendiente = delMes.stream()
                    .filter(this::esDeudaPendiente)
                    .map(Cuota::getImporte)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            resultado.add(new CobranzaMensualResponse(periodo, FechaUtil.NOMBRES_MES[mes - 1], cobrado, pendiente));
        }
        return resultado;
    }

    @Override
    public EstadoSociosResponse obtenerEstadoSocios(CategoriaSocio categoria, TipoPersona tipoPersona) {
        List<Socio> socios = socioRepository.findAll().stream()
                .filter(s -> categoria == null || s.getCategoria() == categoria)
                .filter(s -> tipoPersona == null || s.getTipoPersona() == tipoPersona)
                .toList();

        Map<String, List<Cuota>> cuotasPorSocio = cuotaRepository.findAll().stream()
                .collect(Collectors.groupingBy(Cuota::getSocioId));

        long alDia = 0;
        long pendientes = 0;
        long vencidos = 0;
        long inactivos = 0;
        for (Socio socio : socios) {
            if (socio.getEstado() != EstadoSocio.ACTIVO) {
                inactivos++;
                continue;
            }
            switch (clasificar(cuotasPorSocio.getOrDefault(socio.getId(), List.of()))) {
                case VENCIDO -> vencidos++;
                case PENDIENTE -> pendientes++;
                case AL_DIA -> alDia++;
            }
        }

        return new EstadoSociosResponse(alDia, pendientes, vencidos, inactivos);
    }

    @Override
    public List<UsoBeneficioPorComercioResponse> obtenerUsoBeneficiosPorComercio() {
        Instant inicioMesActual = FechaUtil.inicioDeMesActual();
        Map<String, List<HistorialBeneficio>> porComercio = historialBeneficioRepository.findAll().stream()
                .filter(h -> h.getEstado() == EstadoUsoBeneficio.USADO)
                .collect(Collectors.groupingBy(HistorialBeneficio::getComercioId));

        return porComercio.entrySet().stream()
                .map(entry -> construirFilaComercio(entry.getKey(), entry.getValue(), inicioMesActual))
                .sorted(Comparator.comparingLong(UsoBeneficioPorComercioResponse::cantidadBeneficiosUtilizados).reversed())
                .toList();
    }

    private UsoBeneficioPorComercioResponse construirFilaComercio(
            String comercioId, List<HistorialBeneficio> usos, Instant inicioMesActual) {
        String comercioNombre = usos.get(0).getComercioNombre();

        long cantidadEsteMes = usos.stream()
                .filter(h -> h.getFechaUso() != null && !h.getFechaUso().isBefore(inicioMesActual))
                .count();

        long cantidadSociosUnicos = usos.stream().map(HistorialBeneficio::getSocioId).distinct().count();

        String promocionMasUtilizada = usos.stream()
                .collect(Collectors.groupingBy(HistorialBeneficio::getBeneficioTitulo, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        Map<String, Long> conteoPorPeriodo = usos.stream()
                .filter(h -> h.getFechaUso() != null)
                .collect(Collectors.groupingBy(
                        h -> YearMonth.from(h.getFechaUso().atZone(ZoneId.systemDefault())).toString(),
                        Collectors.counting()));
        List<UsoPeriodoResponse> usoPorPeriodo = conteoPorPeriodo.entrySet().stream()
                .map(e -> new UsoPeriodoResponse(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(UsoPeriodoResponse::periodo))
                .toList();

        return new UsoBeneficioPorComercioResponse(
                comercioId, comercioNombre, usos.size(), cantidadEsteMes,
                cantidadSociosUnicos, promocionMasUtilizada, usoPorPeriodo);
    }

    @Override
    public List<BeneficioMasUtilizadoResponse> obtenerBeneficiosMasUtilizados() {
        Instant inicioMesActual = FechaUtil.inicioDeMesActual();
        Map<String, List<HistorialBeneficio>> porBeneficio = historialBeneficioRepository.findAll().stream()
                .filter(h -> h.getEstado() == EstadoUsoBeneficio.USADO
                        && h.getFechaUso() != null
                        && !h.getFechaUso().isBefore(inicioMesActual))
                .collect(Collectors.groupingBy(HistorialBeneficio::getBeneficioId));

        return porBeneficio.entrySet().stream()
                .map(entry -> {
                    HistorialBeneficio primero = entry.getValue().get(0);
                    return new BeneficioMasUtilizadoResponse(
                            entry.getKey(), primero.getBeneficioTitulo(), primero.getComercioNombre(),
                            entry.getValue().size());
                })
                .sorted(Comparator.comparingLong(BeneficioMasUtilizadoResponse::usosEsteMes).reversed())
                .toList();
    }

    private BigDecimal sumaImporteFacturado(List<Cuota> cuotas, String periodo) {
        return cuotas.stream()
                .filter(c -> periodo.equals(c.getPeriodo()) && c.getEstado() != EstadoCuota.ANULADA)
                .map(Cuota::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean esDeudaPendiente(Cuota cuota) {
        return cuota.getEstado() == EstadoCuota.PENDIENTE
                || cuota.getEstado() == EstadoCuota.VENCIDA
                || cuota.getEstado() == EstadoCuota.EN_REVISION
                || cuota.getEstado() == EstadoCuota.INFORMADA;
    }

    private EstadoCuotaSocio clasificar(List<Cuota> cuotasDelSocio) {
        boolean tieneVencida = cuotasDelSocio.stream().anyMatch(c -> c.getEstado() == EstadoCuota.VENCIDA);
        if (tieneVencida) {
            return EstadoCuotaSocio.VENCIDO;
        }
        boolean tienePendiente = cuotasDelSocio.stream().anyMatch(c ->
                c.getEstado() == EstadoCuota.PENDIENTE
                        || c.getEstado() == EstadoCuota.INFORMADA
                        || c.getEstado() == EstadoCuota.EN_REVISION);
        if (tienePendiente) {
            return EstadoCuotaSocio.PENDIENTE;
        }
        return EstadoCuotaSocio.AL_DIA;
    }

    private enum EstadoCuotaSocio {
        AL_DIA, PENDIENTE, VENCIDO
    }
}
