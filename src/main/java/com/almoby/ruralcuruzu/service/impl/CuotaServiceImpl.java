package com.almoby.ruralcuruzu.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.Cuota;
import com.almoby.ruralcuruzu.domain.DatosPago;
import com.almoby.ruralcuruzu.domain.EjecucionGeneracionCuotas;
import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.domain.TipoCuota;
import com.almoby.ruralcuruzu.dto.request.AnularCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.InformarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RegistrarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RevisarPagoInformadoRequest;
import com.almoby.ruralcuruzu.dto.response.CuotaResponse;
import com.almoby.ruralcuruzu.dto.response.CuotaResumenResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoCuentaSocioResponse;
import com.almoby.ruralcuruzu.dto.response.GeneracionCuotasResponse;
import com.almoby.ruralcuruzu.dto.response.InformarPagoResponse;
import com.almoby.ruralcuruzu.dto.response.RegistrarPagoResponse;
import com.almoby.ruralcuruzu.enums.EstadoCuota;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.EstadoTipoCuota;
import com.almoby.ruralcuruzu.enums.OrigenEjecucionCuotas;
import com.almoby.ruralcuruzu.exception.CuotaEstadoInvalidoException;
import com.almoby.ruralcuruzu.exception.CuotaNoEncontradaException;
import com.almoby.ruralcuruzu.exception.SocioNoEncontradoException;
import com.almoby.ruralcuruzu.repository.CuotaRepository;
import com.almoby.ruralcuruzu.repository.EjecucionGeneracionCuotasRepository;
import com.almoby.ruralcuruzu.repository.SocioRepository;
import com.almoby.ruralcuruzu.repository.TipoCuotaRepository;
import com.almoby.ruralcuruzu.service.CuotaService;
import com.almoby.ruralcuruzu.service.EmailService;

import lombok.extern.slf4j.Slf4j;

/**
 * Ver documento, sección 10 ("Gestión de cuotas"). Reglas clave:
 * - La generación (10.2) es la misma lógica tanto si la dispara el cron
 *   mensual (generarCuotasMensualAutomatico) como si la dispara un admin a
 *   mano (CuotaService.generarCuotas con adminId != null): no se duplica código,
 *   solo cambia el origen que queda registrado en EjecucionGeneracionCuotas.
 * - Si un socio activo no tiene ningún TipoCuota vigente para su categoría, se
 *   lo omite (no se aborta toda la corrida) y queda contado en
 *   cantidadSociosOmitidos para que el admin lo note.
 * - El informe de pago del socio (autoservicio) pasa directo a EN_REVISION:
 *   INFORMADA queda reservado en el enum pero no se usa como estado de reposo
 *   real en este flujo (no hay, por ahora, un paso manual separado entre
 *   "informado" y "en revisión").
 * - VENCIDA se aplica con un job diario (marcarCuotasVencidas), no al vuelo.
 */
@Slf4j
@Service
public class CuotaServiceImpl implements CuotaService {

    private static final Set<EstadoCuota> ESTADOS_QUE_SUMAN_DEUDA =
            EnumSet.of(EstadoCuota.PENDIENTE, EstadoCuota.VENCIDA, EstadoCuota.EN_REVISION);

    private final CuotaRepository cuotaRepository;
    private final TipoCuotaRepository tipoCuotaRepository;
    private final EjecucionGeneracionCuotasRepository ejecucionRepository;
    private final SocioRepository socioRepository;
    private final EmailService emailService;

    public CuotaServiceImpl(CuotaRepository cuotaRepository,
                             TipoCuotaRepository tipoCuotaRepository,
                             EjecucionGeneracionCuotasRepository ejecucionRepository,
                             SocioRepository socioRepository,
                             EmailService emailService) {
        this.cuotaRepository = cuotaRepository;
        this.tipoCuotaRepository = tipoCuotaRepository;
        this.ejecucionRepository = ejecucionRepository;
        this.socioRepository = socioRepository;
        this.emailService = emailService;
    }

    /** Cron mensual: 1º de cada mes a las 6 AM (documento 10.2: "el sistema deberá generar automáticamente"). */
    @Scheduled(cron = "0 0 6 1 * *")
    void generarCuotasMensualAutomatico() {
        log.info("Disparando generación automática mensual de cuotas");
        generarCuotas(null, null, null);
    }

    /** Job diario: pasa a VENCIDA lo que quedó PENDIENTE después de su fecha de vencimiento. */
    @Scheduled(cron = "0 0 1 * * *")
    void marcarCuotasVencidas() {
        List<Cuota> pendientesVencidas =
                cuotaRepository.findByEstadoAndFechaVencimientoBefore(EstadoCuota.PENDIENTE, LocalDate.now());

        for (Cuota cuota : pendientesVencidas) {
            cuota.setEstado(EstadoCuota.VENCIDA);
            cuota.setFechaActualizacion(Instant.now());
            cuotaRepository.save(cuota);
        }

        if (!pendientesVencidas.isEmpty()) {
            log.info("Marcadas {} cuota(s) como VENCIDA", pendientesVencidas.size());
        }
    }

    @Override
    public GeneracionCuotasResponse generarCuotas(String periodoParam, String adminId, String adminNombre) {
        YearMonth periodo = periodoParam != null ? YearMonth.parse(periodoParam) : YearMonth.now();
        String periodoStr = periodo.toString();
        boolean esManual = adminId != null;

        List<Socio> sociosActivos = socioRepository.findByEstado(EstadoSocio.ACTIVO);
        int generadas = 0;
        int omitidos = 0;

        for (Socio socio : sociosActivos) {
            if (cuotaRepository.existsBySocioIdAndPeriodo(socio.getId(), periodoStr)) {
                continue;
            }

            Optional<TipoCuota> tipoCuota = tipoCuotaRepository
                    .findFirstByCategoriaAplicableAndEstadoAndFechaVigenciaLessThanEqualOrderByFechaVigenciaDesc(
                            socio.getCategoria(), EstadoTipoCuota.ACTIVO, LocalDate.now());

            if (tipoCuota.isEmpty()) {
                log.warn("No hay un tipo de cuota vigente para categoria={}: se omite socio id={}",
                        socio.getCategoria(), socio.getId());
                omitidos++;
                continue;
            }

            Cuota cuota = crearCuotaPendiente(socio, tipoCuota.get(), periodo, periodoStr);
            cuotaRepository.save(cuota);
            generadas++;

            String email = socio.obtenerEmail();
            if (email != null) {
                emailService.enviarCorreoCuotaGenerada(
                        email, socio.nombreParaMostrar(), periodoStr, cuota.getImporte(), cuota.getFechaVencimiento());
            }
        }

        EjecucionGeneracionCuotas ejecucion = EjecucionGeneracionCuotas.builder()
                .fechaEjecucion(Instant.now())
                .origen(esManual ? OrigenEjecucionCuotas.MANUAL : OrigenEjecucionCuotas.AUTOMATICA)
                .periodo(periodoStr)
                .cantidadSociosActivos(sociosActivos.size())
                .cantidadCuotasGeneradas(generadas)
                .cantidadSociosOmitidos(omitidos)
                .adminId(adminId)
                .adminNombre(adminNombre)
                .build();
        ejecucionRepository.save(ejecucion);

        log.info("Generación de cuotas ejecutada periodo={} generadas={} omitidos={} origen={}",
                periodoStr, generadas, omitidos, ejecucion.getOrigen());

        return GeneracionCuotasResponse.from(ejecucion);
    }

    private Cuota crearCuotaPendiente(Socio socio, TipoCuota tipoCuota, YearMonth periodo, String periodoStr) {
        int diaVencimiento = Math.min(tipoCuota.getDiaVencimiento(), periodo.lengthOfMonth());
        Instant ahora = Instant.now();

        return Cuota.builder()
                .socioId(socio.getId())
                .socioNumeroSocio(socio.getNumeroSocio())
                .socioNombre(socio.nombreParaMostrar())
                .tipoCuotaId(tipoCuota.getId())
                .tipoCuotaNombre(tipoCuota.getNombre())
                .categoria(socio.getCategoria())
                .periodo(periodoStr)
                .importe(tipoCuota.getImporte())
                .fechaVencimiento(periodo.atDay(diaVencimiento))
                .estado(EstadoCuota.PENDIENTE)
                .fechaGeneracion(ahora)
                .fechaActualizacion(ahora)
                .build();
    }

    @Override
    public List<CuotaResumenResponse> listarCuotas(EstadoCuota estado, String socioId, String periodo) {
        List<Cuota> base;
        if (socioId != null) {
            base = cuotaRepository.findBySocioId(socioId);
        } else if (estado != null) {
            base = cuotaRepository.findByEstado(estado);
        } else {
            base = cuotaRepository.findAll();
        }

        return base.stream()
                .filter(c -> estado == null || c.getEstado() == estado)
                .filter(c -> socioId == null || c.getSocioId().equals(socioId))
                .filter(c -> periodo == null || c.getPeriodo().equals(periodo))
                .map(CuotaResumenResponse::from)
                .toList();
    }

    @Override
    public List<CuotaResumenResponse> listarCuotasDeSocio(String socioId) {
        return cuotaRepository.findBySocioId(socioId).stream().map(CuotaResumenResponse::from).toList();
    }

    @Override
    public CuotaResponse obtenerCuotaPorId(String id) {
        return CuotaResponse.from(buscarOFallar(id));
    }

    @Override
    public RegistrarPagoResponse registrarPago(RegistrarPagoCuotaRequest request, String adminId, String adminNombre) {
        List<Cuota> cuotas = request.cuotaIds().stream().map(this::buscarOFallar).toList();

        for (Cuota cuota : cuotas) {
            validarPuedeRegistrarPago(cuota);

            DatosPago datosPago = DatosPago.builder()
                    .fechaPago(request.fecha().atStartOfDay(ZoneOffset.UTC).toInstant())
                    .importe(request.importe())
                    .medioPago(request.medioPago())
                    .comprobante(request.comprobante())
                    .observacion(request.observacion())
                    .informadoPorSocio(false)
                    .registradoPorAdminId(adminId)
                    .registradoPorAdminNombre(adminNombre)
                    .build();

            cuota.setDatosPago(datosPago);
            cuota.setEstado(EstadoCuota.PAGADA);
            cuota.setFechaActualizacion(Instant.now());
            cuotaRepository.save(cuota);

            log.info("Cuota id={} marcada PAGADA (registro manual, admin={})", cuota.getId(), adminNombre);

            notificarPagoRegistrado(cuota);
        }

        return RegistrarPagoResponse.of(cuotas.stream().map(CuotaResponse::from).toList());
    }

    @Override
    public InformarPagoResponse informarPago(String cuotaId, InformarPagoCuotaRequest request, String socioId) {
        Cuota cuota = buscarOFallar(cuotaId);

        if (!cuota.getSocioId().equals(socioId)) {
            // No revelamos que la cuota existe pero pertenece a otro socio.
            throw new CuotaNoEncontradaException(cuotaId);
        }

        if (cuota.getEstado() != EstadoCuota.PENDIENTE && cuota.getEstado() != EstadoCuota.VENCIDA) {
            throw new CuotaEstadoInvalidoException(
                    "No se puede informar un pago para una cuota en estado " + cuota.getEstado());
        }

        DatosPago datosPago = DatosPago.builder()
                .fechaPago(request.fecha().atStartOfDay(ZoneOffset.UTC).toInstant())
                .importe(request.importe())
                .medioPago(request.medioPago())
                .comprobante(request.comprobante())
                .observacion(request.observacion())
                .informadoPorSocio(true)
                .build();

        cuota.setDatosPago(datosPago);
        cuota.setEstado(EstadoCuota.EN_REVISION);
        cuota.setFechaActualizacion(Instant.now());
        cuotaRepository.save(cuota);

        log.info("Socio id={} informó un pago para cuota id={}", socioId, cuotaId);

        return InformarPagoResponse.of(CuotaResponse.from(cuota));
    }

    @Override
    public CuotaResponse revisarPagoInformado(String cuotaId, RevisarPagoInformadoRequest request,
                                               String adminId, String adminNombre) {
        Cuota cuota = buscarOFallar(cuotaId);

        if (cuota.getEstado() != EstadoCuota.EN_REVISION) {
            throw new CuotaEstadoInvalidoException(
                    "Solo se puede revisar una cuota en estado EN_REVISION (estado actual: " + cuota.getEstado() + ")");
        }

        if (Boolean.TRUE.equals(request.aprobar())) {
            cuota.getDatosPago().setRegistradoPorAdminId(adminId);
            cuota.getDatosPago().setRegistradoPorAdminNombre(adminNombre);
            cuota.setEstado(EstadoCuota.PAGADA);

            log.info("Admin={} aprobó el pago informado de cuota id={}", adminNombre, cuotaId);
            notificarPagoRegistrado(cuota);
        } else {
            if (request.motivoRechazo() == null || request.motivoRechazo().isBlank()) {
                throw new CuotaEstadoInvalidoException("El motivo es obligatorio para rechazar un pago informado");
            }

            cuota.setEstado(EstadoCuota.RECHAZADA);
            cuota.setMotivoRechazo(request.motivoRechazo());

            log.info("Admin={} rechazó el pago informado de cuota id={} motivo={}",
                    adminNombre, cuotaId, request.motivoRechazo());
            notificarPagoRechazado(cuota);
        }

        cuota.setFechaActualizacion(Instant.now());
        cuotaRepository.save(cuota);

        return CuotaResponse.from(cuota);
    }

    @Override
    public CuotaResponse anularCuota(String id, AnularCuotaRequest request, String adminId, String adminNombre) {
        Cuota cuota = buscarOFallar(id);

        if (cuota.getEstado() == EstadoCuota.PAGADA || cuota.getEstado() == EstadoCuota.ANULADA) {
            throw new CuotaEstadoInvalidoException("No se puede anular una cuota en estado " + cuota.getEstado());
        }

        cuota.setEstado(EstadoCuota.ANULADA);
        cuota.setMotivoAnulacion(request.motivo());
        cuota.setFechaActualizacion(Instant.now());
        cuotaRepository.save(cuota);

        log.info("Admin={} anuló la cuota id={} motivo={}", adminNombre, id, request.motivo());

        return CuotaResponse.from(cuota);
    }

    @Override
    public EstadoCuentaSocioResponse obtenerEstadoCuentaSocio(String socioId) {
        Socio socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new SocioNoEncontradoException(socioId));

        List<Cuota> cuotas = cuotaRepository.findBySocioId(socioId);
        BigDecimal deudaTotal = cuotas.stream()
                .filter(c -> ESTADOS_QUE_SUMAN_DEUDA.contains(c.getEstado()))
                .map(Cuota::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new EstadoCuentaSocioResponse(
                socio.getId(),
                socio.getNumeroSocio(),
                socio.nombreParaMostrar(),
                deudaTotal,
                cuotas.stream().map(CuotaResumenResponse::from).toList());
    }

    private void notificarPagoRegistrado(Cuota cuota) {
        Socio socio = socioRepository.findById(cuota.getSocioId()).orElse(null);
        if (socio != null && socio.obtenerEmail() != null) {
            emailService.enviarCorreoPagoRegistrado(
                    socio.obtenerEmail(), socio.nombreParaMostrar(), cuota.getPeriodo(), cuota.getDatosPago().getImporte());
        }
    }

    private void notificarPagoRechazado(Cuota cuota) {
        Socio socio = socioRepository.findById(cuota.getSocioId()).orElse(null);
        if (socio != null && socio.obtenerEmail() != null) {
            emailService.enviarCorreoPagoRechazado(
                    socio.obtenerEmail(), socio.nombreParaMostrar(), cuota.getPeriodo(), cuota.getMotivoRechazo());
        }
    }

    private void validarPuedeRegistrarPago(Cuota cuota) {
        EstadoCuota estado = cuota.getEstado();
        if (estado != EstadoCuota.PENDIENTE && estado != EstadoCuota.VENCIDA && estado != EstadoCuota.EN_REVISION) {
            throw new CuotaEstadoInvalidoException("No se puede registrar un pago para una cuota en estado " + estado);
        }
    }

    private Cuota buscarOFallar(String id) {
        return cuotaRepository.findById(id).orElseThrow(() -> new CuotaNoEncontradaException(id));
    }
}
