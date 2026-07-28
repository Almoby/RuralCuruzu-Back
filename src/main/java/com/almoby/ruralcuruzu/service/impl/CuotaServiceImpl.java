package com.almoby.ruralcuruzu.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.almoby.ruralcuruzu.domain.Cuota;
import com.almoby.ruralcuruzu.domain.EjecucionGeneracionCuotas;
import com.almoby.ruralcuruzu.domain.Pago;
import com.almoby.ruralcuruzu.domain.ReglaCuota;
import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.dto.request.AnularCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.InformarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RegistrarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RevisarPagoInformadoRequest;
import com.almoby.ruralcuruzu.dto.response.CuotaResponse;
import com.almoby.ruralcuruzu.dto.response.CuotaResumenResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoCuentaSocioResponse;
import com.almoby.ruralcuruzu.dto.response.GeneracionCuotasResponse;
import com.almoby.ruralcuruzu.dto.response.InformarPagoResponse;
import com.almoby.ruralcuruzu.dto.response.LinkDePagoResponse;
import com.almoby.ruralcuruzu.dto.response.PagoResponse;
import com.almoby.ruralcuruzu.dto.response.RegistrarPagoResponse;
import com.almoby.ruralcuruzu.dto.response.ResumenCuotasResponse;
import com.almoby.ruralcuruzu.dto.response.RevisarPagoInformadoResponse;
import com.almoby.ruralcuruzu.enums.EstadoCuota;
import com.almoby.ruralcuruzu.enums.EstadoPago;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.MedioPago;
import com.almoby.ruralcuruzu.enums.OrigenEjecucionCuotas;
import com.almoby.ruralcuruzu.exception.ArchivoInvalidoException;
import com.almoby.ruralcuruzu.exception.CuotaEstadoInvalidoException;
import com.almoby.ruralcuruzu.exception.CuotaNoEncontradaException;
import com.almoby.ruralcuruzu.exception.SocioNoEncontradoException;
import com.almoby.ruralcuruzu.repository.CuotaRepository;
import com.almoby.ruralcuruzu.repository.EjecucionGeneracionCuotasRepository;
import com.almoby.ruralcuruzu.repository.PagoRepository;
import com.almoby.ruralcuruzu.repository.ReglaCuotaRepository;
import com.almoby.ruralcuruzu.repository.SocioRepository;
import com.almoby.ruralcuruzu.service.AlmacenamientoComprobantesService;
import com.almoby.ruralcuruzu.service.ComprobanteService;
import com.almoby.ruralcuruzu.service.CuotaService;
import com.almoby.ruralcuruzu.service.EmailService;
import com.almoby.ruralcuruzu.service.EstadoPagoMercadoPago;
import com.almoby.ruralcuruzu.service.MercadoPagoService;
import com.almoby.ruralcuruzu.service.PreferenciaMercadoPago;

import lombok.extern.slf4j.Slf4j;

/**
 * Ver documento, sección 10 ("Gestión de cuotas"). Reglas clave:
 * - La generación (10.2) es la misma lógica tanto si la dispara el cron
 *   mensual (generarCuotasMensualAutomatico) como si la dispara un admin a
 *   mano (CuotaService.generarCuotas con adminId != null): no se duplica código,
 *   solo cambia el origen que queda registrado en EjecucionGeneracionCuotas.
 * - El importe y el día de vencimiento por categoría viven en la colección
 *   {@link ReglaCuota} (reglas_cuota), administrada desde el panel de admin
 *   (ReglaCuotaAdminController). Si un socio activo tiene una categoría sin
 *   regla cargada todavía, se lo omite (no se aborta toda la corrida) y
 *   queda contado en cantidadSociosOmitidos para que el admin lo note.
 * - El informe de pago del socio (autoservicio) pasa directo a EN_REVISION:
 *   INFORMADA queda reservado en el enum pero no se usa como estado de reposo
 *   real en este flujo (no hay, por ahora, un paso manual separado entre
 *   "informado" y "en revisión").
 * - Por qué un registro manual del admin (registrarPago) siempre queda PAGADA
 *   al toque, sin importar el medio, mientras que informarPago siempre
 *   necesita revisión: la diferencia no es el medio de pago, es el canal.
 *   Si el socio paga presencialmente en la oficina ("ventanilla": efectivo,
 *   débito o incluso una transferencia hecha ahí mismo), es el admin quien
 *   cobra y registra el pago directamente, no hace falta confirmar nada.
 *   Si en cambio el socio paga a distancia (transferencia + comprobante
 *   subido desde el sistema), nadie del lado del club vio ese pago todavía,
 *   así que sí o sí pasa por revisión. Por eso informarPago solo acepta
 *   MedioPago.TRANSFERENCIA: es la única forma de pagar a distancia.
 * - VENCIDA se aplica con un job diario (marcarCuotasVencidas), no al vuelo.
 * - RN-17: el pago es su propia entidad de base de datos ({@link Pago}, no
 *   embebido en Cuota): una misma cuota puede tener más de un Pago a lo
 *   largo del tiempo (ej. una transferencia rechazada y un segundo intento
 *   aprobado), y ninguno se sobrescribe ni se borra. Por eso informarPago ya
 *   no queda "trabado" después de un rechazo: RECHAZADA vuelve a admitir un
 *   nuevo intento, que crea un Pago nuevo sin tocar el rechazado anterior.
 */
@Slf4j
@Service
public class CuotaServiceImpl implements CuotaService {

    private static final Set<EstadoCuota> ESTADOS_QUE_SUMAN_DEUDA =
            EnumSet.of(EstadoCuota.PENDIENTE, EstadoCuota.VENCIDA, EstadoCuota.EN_REVISION);

    private final CuotaRepository cuotaRepository;
    private final PagoRepository pagoRepository;
    private final EjecucionGeneracionCuotasRepository ejecucionRepository;
    private final SocioRepository socioRepository;
    private final EmailService emailService;
    private final ReglaCuotaRepository reglaCuotaRepository;
    private final AlmacenamientoComprobantesService almacenamientoComprobantesService;
    private final MercadoPagoService mercadoPagoService;
    private final ComprobanteService comprobanteService;

    public CuotaServiceImpl(CuotaRepository cuotaRepository,
                             PagoRepository pagoRepository,
                             EjecucionGeneracionCuotasRepository ejecucionRepository,
                             SocioRepository socioRepository,
                             EmailService emailService,
                             ReglaCuotaRepository reglaCuotaRepository,
                             AlmacenamientoComprobantesService almacenamientoComprobantesService,
                             MercadoPagoService mercadoPagoService,
                             ComprobanteService comprobanteService) {
        this.cuotaRepository = cuotaRepository;
        this.pagoRepository = pagoRepository;
        this.ejecucionRepository = ejecucionRepository;
        this.socioRepository = socioRepository;
        this.emailService = emailService;
        this.reglaCuotaRepository = reglaCuotaRepository;
        this.almacenamientoComprobantesService = almacenamientoComprobantesService;
        this.mercadoPagoService = mercadoPagoService;
        this.comprobanteService = comprobanteService;
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

            Optional<ReglaCuota> regla = reglaCuotaRepository.findByCategoriaAplicable(socio.getCategoria());

            if (regla.isEmpty()) {
                log.warn("No hay una regla de cuota configurada para categoria={}: se omite socio id={}",
                        socio.getCategoria(), socio.getId());
                omitidos++;
                continue;
            }

            Cuota cuota = crearCuotaPendiente(socio, regla.get(), periodo, periodoStr);
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

    private Cuota crearCuotaPendiente(Socio socio, ReglaCuota regla, YearMonth periodo, String periodoStr) {
        int diaVencimiento = Math.min(regla.getDiaVencimiento(), periodo.lengthOfMonth());
        Instant ahora = Instant.now();

        return Cuota.builder()
                .socioId(socio.getId())
                .socioNumeroSocio(socio.getNumeroSocio())
                .socioNombre(socio.nombreParaMostrar())
                .tipoCuotaNombre(regla.getNombre())
                .categoria(socio.getCategoria())
                .periodo(periodoStr)
                .importe(regla.getImporte())
                .fechaVencimiento(periodo.atDay(diaVencimiento))
                .estado(EstadoCuota.PENDIENTE)
                .fechaGeneracion(ahora)
                .fechaActualizacion(ahora)
                .build();
    }

    @Override
    public List<GeneracionCuotasResponse> listarEjecuciones() {
        return ejecucionRepository.findAllByOrderByFechaEjecucionDesc().stream()
                .map(GeneracionCuotasResponse::from)
                .toList();
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

        List<Cuota> filtradas = base.stream()
                .filter(c -> estado == null || c.getEstado() == estado)
                .filter(c -> socioId == null || c.getSocioId().equals(socioId))
                .filter(c -> periodo == null || c.getPeriodo().equals(periodo))
                .toList();

        Map<String, Pago> pagoVigentePorCuota = pagosVigentesPorCuotaId(idsDe(filtradas));
        return filtradas.stream()
                .map(c -> CuotaResumenResponse.from(c, pagoVigentePorCuota.get(c.getId())))
                .toList();
    }

    @Override
    public List<CuotaResumenResponse> listarCuotasDeSocio(String socioId) {
        List<Cuota> cuotas = cuotaRepository.findBySocioId(socioId);
        Map<String, Pago> pagoVigentePorCuota = pagosVigentesPorCuotaId(idsDe(cuotas));
        return cuotas.stream()
                .map(c -> CuotaResumenResponse.from(c, pagoVigentePorCuota.get(c.getId())))
                .toList();
    }

    @Override
    public List<PagoResponse> listarPagosDeSocio(String socioId) {
        return pagoRepository.findBySocioIdOrderByFechaCreacionDesc(socioId).stream()
                .map(PagoResponse::from)
                .toList();
    }

    @Override
    public ResumenCuotasResponse obtenerResumen() {
        List<Cuota> todas = cuotaRepository.findAll();
        Map<String, Pago> pagoAprobadoPorCuota = pagoRepository.findByEstado(EstadoPago.APROBADO).stream()
                .collect(Collectors.toMap(Pago::getCuotaId, pago -> pago, (a, b) -> a));

        BigDecimal totalCobrado = sumaPagada(todas, pagoAprobadoPorCuota, pago -> true);
        BigDecimal totalCobradoEnEfectivo = sumaPagada(todas, pagoAprobadoPorCuota,
                pago -> pago != null && pago.getMedioPago() == MedioPago.EFECTIVO);
        BigDecimal totalEnRevision = todas.stream()
                .filter(cuota -> cuota.getEstado() == EstadoCuota.EN_REVISION)
                .map(Cuota::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long cantidadPendientes = todas.stream().filter(this::cuentaComoPendiente).count();
        long cantidadAprobadas = todas.stream().filter(cuota -> cuota.getEstado() == EstadoCuota.PAGADA).count();
        long cantidadRechazadas = todas.stream().filter(cuota -> cuota.getEstado() == EstadoCuota.RECHAZADA).count();

        return new ResumenCuotasResponse(totalCobrado, totalEnRevision, totalCobradoEnEfectivo,
                todas.size(), cantidadPendientes, cantidadAprobadas, cantidadRechazadas);
    }

    /** "Pendientes" agrupa todo lo que todavía no se resolvió: recién generada, vencida o esperando revisión. */
    private boolean cuentaComoPendiente(Cuota cuota) {
        return cuota.getEstado() == EstadoCuota.PENDIENTE
                || cuota.getEstado() == EstadoCuota.VENCIDA
                || cuota.getEstado() == EstadoCuota.EN_REVISION;
    }

    /** Suma el importe efectivamente pagado (pago.importe) de las cuotas PAGADA que cumplan el filtro. */
    private BigDecimal sumaPagada(List<Cuota> cuotas, Map<String, Pago> pagoAprobadoPorCuota,
                                   Predicate<Pago> filtroAdicional) {
        return cuotas.stream()
                .filter(cuota -> cuota.getEstado() == EstadoCuota.PAGADA)
                .map(cuota -> pagoAprobadoPorCuota.get(cuota.getId()))
                .filter(filtroAdicional)
                .map(pago -> pago != null ? pago.getImporte() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public CuotaResponse obtenerCuotaPorId(String id) {
        Cuota cuota = buscarOFallar(id);
        return CuotaResponse.from(cuota, pagoVigentePara(cuota.getId()));
    }

    @Override
    public RegistrarPagoResponse registrarPago(RegistrarPagoCuotaRequest request, String adminId, String adminNombre) {
        List<Cuota> cuotas = request.periodos().stream()
                .map(periodo -> buscarCuotaDeSocioEnPeriodo(request.socioId(), periodo))
                .toList();

        List<CuotaResponse> respuestas = new ArrayList<>();
        for (Cuota cuota : cuotas) {
            validarPuedeRegistrarPago(cuota);

            Instant ahora = Instant.now();
            // El importe pagado es el de la propia cuota (fijado al generarla, según la
            // regla de cuota vigente para su categoría en ese momento), no un valor que
            // tipee el admin: así no se puede registrar un pago por un monto que no
            // coincida con lo adeudado.
            Pago pago = Pago.builder()
                    .cuotaId(cuota.getId())
                    .socioId(cuota.getSocioId())
                    .socioNumeroSocio(cuota.getSocioNumeroSocio())
                    .socioNombre(cuota.getSocioNombre())
                    .periodo(cuota.getPeriodo())
                    .fechaPago(request.fecha().atStartOfDay(ZoneOffset.UTC).toInstant())
                    .importe(cuota.getImporte())
                    .medioPago(request.medioPago())
                    .comprobanteRuta(request.comprobante())
                    .observacion(request.observacion())
                    .estado(EstadoPago.APROBADO)
                    .informadoPorSocio(false)
                    .registradoPorAdminId(adminId)
                    .registradoPorAdminNombre(adminNombre)
                    .fechaCreacion(ahora)
                    .fechaActualizacion(ahora)
                    .build();
            pagoRepository.save(pago);

            cuota.setEstado(EstadoCuota.PAGADA);
            cuota.setFechaActualizacion(ahora);
            cuotaRepository.save(cuota);

            log.info("Cuota id={} marcada PAGADA (registro manual, admin={})", cuota.getId(), adminNombre);

            notificarPagoRegistrado(cuota, pago);
            respuestas.add(CuotaResponse.from(cuota, pago));
        }

        return RegistrarPagoResponse.of(respuestas);
    }

    @Override
    public InformarPagoResponse informarPago(String cuotaId, InformarPagoCuotaRequest request,
                                              MultipartFile comprobante, String socioId) {
        Cuota cuota = buscarOFallar(cuotaId);

        if (!cuota.getSocioId().equals(socioId)) {
            // No revelamos que la cuota existe pero pertenece a otro socio.
            throw new CuotaNoEncontradaException(cuotaId);
        }

        if (request.medioPago() != MedioPago.TRANSFERENCIA) {
            // La única forma de pagar a distancia por autoservicio "informando" un pago
            // es transferencia + comprobante; el link de pago tiene su propio flujo
            // (CuotaService.generarLinkDePago), y efectivo/débito/ventanilla se pagan y
            // registran presencialmente en la oficina (CuotaServiceImpl.registrarPago).
            throw new CuotaEstadoInvalidoException(
                    "Solo se puede informar un pago por transferencia; los demás medios se pagan y "
                            + "registran presencialmente en la oficina");
        }

        // RN-17: un Pago es su propia entidad, así que un rechazo previo no deja la
        // cuota trabada: RECHAZADA vuelve a admitir un nuevo intento, que crea un
        // Pago nuevo sin tocar el rechazado anterior (que sigue existiendo como
        // historial).
        validarEstadoAdmiteNuevoIntentoDePago(cuota);

        if (comprobante == null || comprobante.isEmpty()) {
            throw new ArchivoInvalidoException("El comprobante de la transferencia es obligatorio");
        }

        Instant ahora = Instant.now();
        Pago pago = Pago.builder()
                .cuotaId(cuota.getId())
                .socioId(cuota.getSocioId())
                .socioNumeroSocio(cuota.getSocioNumeroSocio())
                .socioNombre(cuota.getSocioNombre())
                .periodo(cuota.getPeriodo())
                .fechaPago(request.fecha().atStartOfDay(ZoneOffset.UTC).toInstant())
                .importe(request.importe())
                .medioPago(request.medioPago())
                .observacion(request.observacion())
                .estado(EstadoPago.EN_REVISION)
                .informadoPorSocio(true)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .build();
        // Se guarda antes de subir el archivo porque el comprobante se guarda en una
        // subcarpeta con el id del pago, y Mongo recién lo asigna al persistir.
        pagoRepository.save(pago);

        String comprobanteRuta = almacenamientoComprobantesService.guardar(pago.getId(), comprobante);
        pago.setComprobanteRuta(comprobanteRuta);
        pagoRepository.save(pago);

        // Comprobante como su propia entidad (documento 10.4): además del dato en
        // Pago.comprobanteRuta (compatibilidad), queda registrado acá con su metadata.
        comprobanteService.registrarSubidoPorSocio(pago, comprobanteRuta, comprobante);

        cuota.setEstado(EstadoCuota.EN_REVISION);
        cuota.setMotivoRechazo(null);
        cuota.setFechaActualizacion(ahora);
        cuotaRepository.save(cuota);

        log.info("Socio id={} informó un pago para cuota id={}", socioId, cuotaId);

        return InformarPagoResponse.of(CuotaResponse.from(cuota, pago));
    }

    @Override
    public RevisarPagoInformadoResponse revisarPagoInformado(String cuotaId, RevisarPagoInformadoRequest request,
                                                              String adminId, String adminNombre) {
        Cuota cuota = buscarOFallar(cuotaId);

        if (cuota.getEstado() != EstadoCuota.EN_REVISION) {
            throw new CuotaEstadoInvalidoException(
                    "Solo se puede revisar una cuota en estado EN_REVISION (estado actual: " + cuota.getEstado() + ")");
        }

        Pago pago = pagoRepository.findByCuotaIdAndEstado(cuotaId, EstadoPago.EN_REVISION)
                .orElseThrow(() -> new CuotaEstadoInvalidoException(
                        "No hay ningún pago en revisión para esta cuota"));

        Instant ahora = Instant.now();
        // Qué Pago va en la respuesta como "pagoVigente": solo el recién aprobado
        // (que sí es genuinamente vigente); uno recién rechazado no lo es (ver el
        // contrato documentado en CuotaResponse: vigente = APROBADO o EN_REVISION,
        // si no, null), así que la respuesta de un rechazo no lo incluye ahí — el
        // motivo del rechazo se ve igual en CuotaResponse.motivoRechazo.
        Pago pagoParaLaRespuesta;
        if (Boolean.TRUE.equals(request.aprobar())) {
            pago.setEstado(EstadoPago.APROBADO);
            pago.setRegistradoPorAdminId(adminId);
            pago.setRegistradoPorAdminNombre(adminNombre);
            pago.setFechaActualizacion(ahora);
            pagoRepository.save(pago);

            cuota.setEstado(EstadoCuota.PAGADA);
            pagoParaLaRespuesta = pago;

            log.info("Admin={} aprobó el pago informado de cuota id={}", adminNombre, cuotaId);
            notificarPagoRegistrado(cuota, pago);
        } else {
            if (request.motivoRechazo() == null || request.motivoRechazo().isBlank()) {
                throw new CuotaEstadoInvalidoException("El motivo es obligatorio para rechazar un pago informado");
            }

            pago.setEstado(EstadoPago.RECHAZADO);
            pago.setMotivoRechazo(request.motivoRechazo());
            pago.setRegistradoPorAdminId(adminId);
            pago.setRegistradoPorAdminNombre(adminNombre);
            pago.setFechaActualizacion(ahora);
            pagoRepository.save(pago);

            cuota.setEstado(EstadoCuota.RECHAZADA);
            cuota.setMotivoRechazo(request.motivoRechazo());
            pagoParaLaRespuesta = null;

            log.info("Admin={} rechazó el pago informado de cuota id={} motivo={}",
                    adminNombre, cuotaId, request.motivoRechazo());
            notificarPagoRechazado(cuota);
        }

        cuota.setFechaActualizacion(ahora);
        cuotaRepository.save(cuota);

        return RevisarPagoInformadoResponse.of(
                CuotaResponse.from(cuota, pagoParaLaRespuesta), Boolean.TRUE.equals(request.aprobar()));
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

        return CuotaResponse.from(cuota, pagoVigentePara(id));
    }

    @Override
    public LinkDePagoResponse generarLinkDePago(String cuotaId, String socioId) {
        Cuota cuota = buscarOFallar(cuotaId);

        if (!cuota.getSocioId().equals(socioId)) {
            throw new CuotaNoEncontradaException(cuotaId);
        }

        // Mismos estados que informarPago: RN-17 hace que un rechazo previo (ya
        // sea de una transferencia o de un intento anterior de link de pago) no
        // deje la cuota trabada, siempre se puede volver a intentar.
        validarEstadoAdmiteNuevoIntentoDePago(cuota);

        Instant ahora = Instant.now();
        Pago pago = Pago.builder()
                .cuotaId(cuota.getId())
                .socioId(cuota.getSocioId())
                .socioNumeroSocio(cuota.getSocioNumeroSocio())
                .socioNombre(cuota.getSocioNombre())
                .periodo(cuota.getPeriodo())
                .importe(cuota.getImporte())
                .medioPago(MedioPago.LINK_DE_PAGO)
                .estado(EstadoPago.EN_REVISION)
                .informadoPorSocio(true)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .build();
        // Igual que en informarPago: se guarda primero para tener el id del Pago,
        // que viaja como external_reference a Mercado Pago (así el webhook puede
        // encontrar de nuevo este mismo Pago).
        pagoRepository.save(pago);

        String descripcion = "Cuota " + cuota.getPeriodo() + " - " + cuota.getSocioNombre();
        PreferenciaMercadoPago preferencia =
                mercadoPagoService.crearPreferencia(pago.getId(), descripcion, cuota.getImporte());

        pago.setMercadoPagoPreferenceId(preferencia.preferenceId());
        pagoRepository.save(pago);

        cuota.setEstado(EstadoCuota.EN_REVISION);
        cuota.setMotivoRechazo(null);
        cuota.setFechaActualizacion(ahora);
        cuotaRepository.save(cuota);

        log.info("Socio id={} generó un link de pago para cuota id={} pagoId={} preferenceId={}",
                socioId, cuotaId, pago.getId(), preferencia.preferenceId());

        return LinkDePagoResponse.of(pago.getId(), preferencia.initPoint());
    }

    @Override
    public void procesarNotificacionMercadoPago(String mercadoPagoPaymentId) {
        // Nunca se confía en el contenido del webhook: siempre se reconsulta el
        // estado real del pago contra la propia API de Mercado Pago.
        EstadoPagoMercadoPago estadoReal = mercadoPagoService.consultarPago(mercadoPagoPaymentId);

        String pagoId = estadoReal.externalReference();
        if (pagoId == null) {
            log.warn("Notificación de Mercado Pago sin external_reference (paymentId={}), se ignora",
                    mercadoPagoPaymentId);
            return;
        }

        Pago pago = pagoRepository.findById(pagoId).orElse(null);
        if (pago == null) {
            log.warn("Notificación de Mercado Pago para un Pago inexistente (pagoId={}, paymentId={}), se ignora",
                    pagoId, mercadoPagoPaymentId);
            return;
        }

        // Idempotencia: si este Pago ya quedó resuelto (por una notificación
        // anterior, ya sea aprobado o rechazado), una notificación repetida no
        // hace nada más.
        if (pago.getEstado() != EstadoPago.EN_REVISION) {
            log.info("Pago id={} ya estaba resuelto (estado={}), se ignora notificación repetida de Mercado Pago",
                    pagoId, pago.getEstado());
            return;
        }

        Cuota cuota = cuotaRepository.findById(pago.getCuotaId()).orElse(null);
        if (cuota == null) {
            log.warn("Pago id={} referencia una cuota inexistente (cuotaId={}), se ignora", pagoId, pago.getCuotaId());
            return;
        }

        Instant ahora = Instant.now();
        // El id de Mercado Pago se registra siempre (sirve para trazabilidad aunque
        // todavía esté pending), pero fechaPago (la fecha real de la transacción)
        // recién se fija cuando el resultado es definitivo: no tendría sentido decir
        // que un pago "pending" ya se pagó.
        pago.setMercadoPagoPaymentId(estadoReal.mercadoPagoPaymentId());
        pago.setFechaActualizacion(ahora);

        if (estadoReal.aprobado()) {
            pago.setEstado(EstadoPago.APROBADO);
            pago.setFechaPago(ahora);
            pagoRepository.save(pago);

            cuota.setEstado(EstadoCuota.PAGADA);
            cuota.setFechaActualizacion(ahora);
            cuotaRepository.save(cuota);

            log.info("Mercado Pago aprobó el pago id={} de cuota id={}", pagoId, cuota.getId());
            notificarPagoRegistrado(cuota, pago);
        } else if (estadoReal.rechazado()) {
            pago.setEstado(EstadoPago.RECHAZADO);
            pago.setFechaPago(ahora);
            pago.setMotivoRechazo("Pago rechazado por Mercado Pago");
            pagoRepository.save(pago);

            // Igual que un rechazo de transferencia (RN-17): el socio puede
            // volver a intentar, ya sea con un nuevo link de pago o transfiriendo.
            cuota.setEstado(EstadoCuota.RECHAZADA);
            cuota.setMotivoRechazo("Pago rechazado por Mercado Pago");
            cuota.setFechaActualizacion(ahora);
            cuotaRepository.save(cuota);

            log.info("Mercado Pago rechazó el pago id={} de cuota id={}", pagoId, cuota.getId());
            notificarPagoRechazado(cuota);
        } else {
            // "pending" / "in_process": todavía no hay nada definitivo, solo se
            // deja registrado el id de Mercado Pago para la próxima consulta.
            pagoRepository.save(pago);
            log.info("Pago id={} sigue pendiente en Mercado Pago (status={})", pagoId, estadoReal.status());
        }
    }

    /** Válido tanto para informarPago (transferencia) como para generarLinkDePago (Mercado Pago). */
    private void validarEstadoAdmiteNuevoIntentoDePago(Cuota cuota) {
        if (cuota.getEstado() != EstadoCuota.PENDIENTE && cuota.getEstado() != EstadoCuota.VENCIDA
                && cuota.getEstado() != EstadoCuota.RECHAZADA) {
            throw new CuotaEstadoInvalidoException(
                    "No se puede intentar un nuevo pago para una cuota en estado " + cuota.getEstado());
        }
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

        Map<String, Pago> pagoVigentePorCuota = pagosVigentesPorCuotaId(idsDe(cuotas));

        return new EstadoCuentaSocioResponse(
                socio.getId(),
                socio.getNumeroSocio(),
                socio.nombreParaMostrar(),
                deudaTotal,
                cuotas.stream().map(c -> CuotaResumenResponse.from(c, pagoVigentePorCuota.get(c.getId()))).toList());
    }

    private void notificarPagoRegistrado(Cuota cuota, Pago pago) {
        Socio socio = socioRepository.findById(cuota.getSocioId()).orElse(null);
        if (socio != null && socio.obtenerEmail() != null) {
            emailService.enviarCorreoPagoRegistrado(
                    socio.obtenerEmail(), socio.nombreParaMostrar(), cuota.getPeriodo(), pago.getImporte());
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

    private Cuota buscarCuotaDeSocioEnPeriodo(String socioId, String periodo) {
        return cuotaRepository.findBySocioIdAndPeriodo(socioId, periodo)
                .orElseThrow(() -> CuotaNoEncontradaException.paraSocioYPeriodo(socioId, periodo));
    }

    private List<String> idsDe(List<Cuota> cuotas) {
        return cuotas.stream().map(Cuota::getId).toList();
    }

    /**
     * El pago vigente de UNA cuota: el APROBADO si existe, si no el EN_REVISION,
     * si no {@code null}. Para listados de varias cuotas usar
     * {@link #pagosVigentesPorCuotaId} (evita N+1).
     */
    private Pago pagoVigentePara(String cuotaId) {
        return pagoRepository.findByCuotaIdAndEstado(cuotaId, EstadoPago.APROBADO)
                .or(() -> pagoRepository.findByCuotaIdAndEstado(cuotaId, EstadoPago.EN_REVISION))
                .orElse(null);
    }

    /** Igual que {@link #pagoVigentePara}, pero para varias cuotas en una sola consulta. */
    private Map<String, Pago> pagosVigentesPorCuotaId(List<String> cuotaIds) {
        if (cuotaIds.isEmpty()) {
            return Map.of();
        }
        Map<String, List<Pago>> pagosPorCuota = pagoRepository.findByCuotaIdIn(cuotaIds).stream()
                .collect(Collectors.groupingBy(Pago::getCuotaId));

        Map<String, Pago> resultado = new HashMap<>();
        pagosPorCuota.forEach((cuotaId, pagos) -> {
            Optional<Pago> vigente = pagos.stream().filter(p -> p.getEstado() == EstadoPago.APROBADO).findFirst()
                    .or(() -> pagos.stream().filter(p -> p.getEstado() == EstadoPago.EN_REVISION).findFirst());
            vigente.ifPresent(pago -> resultado.put(cuotaId, pago));
        });
        return resultado;
    }
}
