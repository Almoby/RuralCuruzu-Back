package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.domain.Comercio;
import com.almoby.ruralcuruzu.domain.HistorialBeneficio;
import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.domain.TipoBeneficioCatalogo;
import com.almoby.ruralcuruzu.dto.request.ActualizarBeneficioRequest;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoBeneficioRequest;
import com.almoby.ruralcuruzu.dto.request.CrearBeneficioRequest;
import com.almoby.ruralcuruzu.dto.request.ValidarBeneficioRequest;
import com.almoby.ruralcuruzu.dto.response.BeneficioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.BeneficioResponse;
import com.almoby.ruralcuruzu.dto.response.BeneficioResumenResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioConBeneficiosResponse;
import com.almoby.ruralcuruzu.dto.response.HistorialBeneficioResponse;
import com.almoby.ruralcuruzu.dto.response.ValidarBeneficioResponse;
import com.almoby.ruralcuruzu.enums.EstadoBeneficio;
import com.almoby.ruralcuruzu.enums.EstadoUsoBeneficio;
import com.almoby.ruralcuruzu.exception.BeneficioNoEncontradoException;
import com.almoby.ruralcuruzu.exception.BeneficioNoVigenteException;
import com.almoby.ruralcuruzu.exception.BeneficioYaCanjeadoException;
import com.almoby.ruralcuruzu.exception.CodigoQrInvalidoException;
import com.almoby.ruralcuruzu.exception.ComercioNoEncontradoException;
import com.almoby.ruralcuruzu.exception.TipoBeneficioInvalidoException;
import com.almoby.ruralcuruzu.repository.BeneficioRepository;
import com.almoby.ruralcuruzu.repository.ComercioRepository;
import com.almoby.ruralcuruzu.repository.HistorialBeneficioRepository;
import com.almoby.ruralcuruzu.repository.SocioRepository;
import com.almoby.ruralcuruzu.repository.TipoBeneficioCatalogoRepository;
import com.almoby.ruralcuruzu.security.jwt.QrTokenService;
import com.almoby.ruralcuruzu.service.BeneficioService;
import com.almoby.ruralcuruzu.service.EstadoQrService;
import com.almoby.ruralcuruzu.util.FechaUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Ver documento, sección 14. Reglas clave (confirmadas por el dueño del
 * proyecto):
 * - Los beneficios los crea y administra el propio comercio, no el admin.
 * - El uso se valida en el momento: el socio muestra su QR, el comercio lo
 *   escanea; si es válido se aplica el descuento y queda un registro para
 *   ambos. No hay paso de revisión posterior (a diferencia de las cuotas
 *   informadas por el socio).
 * - "Estado de la operación" es simple: USADO (éxito) o el pedido se
 *   rechaza directamente con un error (QR inválido, beneficio no vigente,
 *   beneficio de otro comercio, beneficio ya canjeado antes por ese socio)
 *   sin llegar a crear un registro.
 * - Cada beneficio se puede canjear una única vez por socio, para siempre
 *   (no por día): no es un descuento recurrente, es más parecido a un cupón.
 */
@Slf4j
@Service
public class BeneficioServiceImpl implements BeneficioService {

    private final BeneficioRepository beneficioRepository;
    private final HistorialBeneficioRepository historialBeneficioRepository;
    private final ComercioRepository comercioRepository;
    private final SocioRepository socioRepository;
    private final EstadoQrService estadoQrService;
    private final QrTokenService qrTokenService;
    private final TipoBeneficioCatalogoRepository tipoBeneficioCatalogoRepository;

    public BeneficioServiceImpl(BeneficioRepository beneficioRepository,
                                 HistorialBeneficioRepository historialBeneficioRepository,
                                 ComercioRepository comercioRepository,
                                 SocioRepository socioRepository,
                                 EstadoQrService estadoQrService,
                                 QrTokenService qrTokenService,
                                 TipoBeneficioCatalogoRepository tipoBeneficioCatalogoRepository) {
        this.beneficioRepository = beneficioRepository;
        this.historialBeneficioRepository = historialBeneficioRepository;
        this.comercioRepository = comercioRepository;
        this.socioRepository = socioRepository;
        this.tipoBeneficioCatalogoRepository = tipoBeneficioCatalogoRepository;
        this.estadoQrService = estadoQrService;
        this.qrTokenService = qrTokenService;
    }

    /**
     * Job diario, a la medianoche exacta (el instante en que, por definición,
     * deja de estar vigente un beneficio cuya fechaFinVigencia fue ayer):
     * corrige el dato crudo en la base para que quede en sync con
     * {@link Beneficio#estadoEfectivo()}, que es lo que ya ven el front y
     * cualquier consumidor de la API sin depender de este job (ese método
     * recalcula la vigencia en el momento, siempre). Este job es "housekeeping"
     * para que también quede correcto quien mire la colección directamente
     * (ej. desde Atlas). Un beneficio sin fechaFinVigencia (vigente para
     * siempre) nunca entra acá.
     */
    @Scheduled(cron = "0 0 0 * * *")
    void marcarBeneficiosVencidos() {
        List<Beneficio> vencidos =
                beneficioRepository.findByEstadoAndFechaFinVigenciaBefore(EstadoBeneficio.ACTIVO, LocalDate.now());

        for (Beneficio beneficio : vencidos) {
            beneficio.setEstado(EstadoBeneficio.INACTIVO);
            beneficio.setFechaActualizacion(Instant.now());
            beneficioRepository.save(beneficio);
        }

        if (!vencidos.isEmpty()) {
            log.info("Marcados {} beneficio(s) como INACTIVO por fin de vigencia", vencidos.size());
        }
    }

    /**
     * Job diario simétrico al de arriba, para la otra punta: un beneficio
     * creado con fechaInicioVigencia futura nace INACTIVO en la base (ver
     * crearBeneficio) y este job lo pasa a ACTIVO apenas llega ese día,
     * también a la medianoche exacta. pausadoManualmente=false en la consulta
     * asegura que nunca se reactive algo que el comercio pausó a propósito.
     */
    @Scheduled(cron = "0 0 0 * * *")
    void activarBeneficiosQueEmpiezanHoy() {
        List<Beneficio> paraActivar = beneficioRepository
                .findByEstadoAndPausadoManualmenteFalseAndFechaInicioVigenciaLessThanEqual(
                        EstadoBeneficio.INACTIVO, LocalDate.now())
                .stream()
                // Por si ya pasó también su fechaFinVigencia (ventana de vigencia entera en
                // el pasado): no tiene sentido activarlo para que el otro job lo vuelva a
                // apagar recién a la medianoche siguiente.
                .filter(Beneficio::dentroDeVigenciaHoy)
                .toList();

        for (Beneficio beneficio : paraActivar) {
            beneficio.setEstado(EstadoBeneficio.ACTIVO);
            beneficio.setFechaActualizacion(Instant.now());
            beneficioRepository.save(beneficio);
        }

        if (!paraActivar.isEmpty()) {
            log.info("Marcados {} beneficio(s) como ACTIVO por inicio de vigencia", paraActivar.size());
        }
    }

    @Override
    public BeneficioCreadoResponse crearBeneficio(String comercioId, CrearBeneficioRequest request) {
        Comercio comercio = comercioRepository.findById(comercioId)
                .orElseThrow(() -> new ComercioNoEncontradoException(comercioId));
        TipoBeneficioCatalogo tipoBeneficio = resolverTipoBeneficioActivo(request.tipoBeneficioId());

        Instant ahora = Instant.now();
        Beneficio beneficio = Beneficio.builder()
                .comercioId(comercio.getId())
                .comercioNombre(comercio.getNombreComercial())
                .comercioRubro(comercio.getRubro())
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .tipoBeneficioId(tipoBeneficio.getId())
                .tipoBeneficioNombre(tipoBeneficio.getNombre())
                .valor(request.valor())
                .fechaInicioVigencia(request.fechaInicioVigencia())
                .fechaFinVigencia(request.fechaFinVigencia())
                .pausadoManualmente(false)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .build();
        // Si la fecha de inicio es futura, nace INACTIVO en la base (no solo en el
        // estadoEfectivo() de la API): el job de arriba lo activa solo cuando llegue el día.
        beneficio.setEstado(beneficio.dentroDeVigenciaHoy() ? EstadoBeneficio.ACTIVO : EstadoBeneficio.INACTIVO);
        beneficioRepository.save(beneficio);

        log.info("Comercio id={} creó el beneficio id={} ({})", comercioId, beneficio.getId(), request.titulo());

        return BeneficioCreadoResponse.of(BeneficioResponse.from(beneficio));
    }

    @Override
    public List<BeneficioResponse> listarBeneficiosDelComercio(String comercioId) {
        List<Beneficio> beneficios = beneficioRepository.findByComercioId(comercioId);

        // Una sola consulta para todos los usos del mes del comercio, en vez de una por beneficio.
        Map<String, Long> usosEsteMesPorBeneficio = historialBeneficioRepository
                .findByComercioIdAndFechaUsoAfter(comercioId, FechaUtil.inicioDeMesActual()).stream()
                .collect(Collectors.groupingBy(HistorialBeneficio::getBeneficioId, Collectors.counting()));

        return beneficios.stream()
                .map(b -> BeneficioResponse.from(b, usosEsteMesPorBeneficio.getOrDefault(b.getId(), 0L)))
                .toList();
    }

    @Override
    public BeneficioResponse obtenerBeneficioDelComercio(String comercioId, String beneficioId) {
        Beneficio beneficio = buscarPropioOFallar(comercioId, beneficioId);
        return BeneficioResponse.from(beneficio, usosEsteMes(beneficioId));
    }

    @Override
    public BeneficioResponse actualizarBeneficio(String comercioId, String beneficioId,
                                                  ActualizarBeneficioRequest request) {
        Beneficio beneficio = buscarPropioOFallar(comercioId, beneficioId);
        TipoBeneficioCatalogo tipoBeneficio = resolverTipoBeneficioActivo(request.tipoBeneficioId());

        beneficio.setTitulo(request.titulo());
        beneficio.setDescripcion(request.descripcion());
        beneficio.setTipoBeneficioId(tipoBeneficio.getId());
        beneficio.setTipoBeneficioNombre(tipoBeneficio.getNombre());
        beneficio.setValor(request.valor());
        beneficio.setFechaInicioVigencia(request.fechaInicioVigencia());
        beneficio.setFechaFinVigencia(request.fechaFinVigencia());

        // El campo crudo se auto-sincroniza con las fechas nuevas, salvo que el comercio lo
        // haya pausado a mano (pausadoManualmente): eso es una decisión explícita que una
        // edición de fechas no debería pisar. Sin esta marca, esto cubre los dos sentidos:
        // reactiva solo si quedó vigente (venció y le extendieron la fecha de fin, o antes
        // no había llegado la fecha de inicio y ahora sí), y desactiva solo si quedó fuera de
        // rango (ej. adelantaron la fecha de inicio a la semana que viene).
        if (!beneficio.isPausadoManualmente()) {
            beneficio.setEstado(beneficio.dentroDeVigenciaHoy() ? EstadoBeneficio.ACTIVO : EstadoBeneficio.INACTIVO);
        }

        beneficio.setFechaActualizacion(Instant.now());
        beneficioRepository.save(beneficio);

        log.info("Comercio id={} actualizó el beneficio id={}", comercioId, beneficioId);

        return BeneficioResponse.from(beneficio, usosEsteMes(beneficioId));
    }

    @Override
    public BeneficioResponse cambiarEstadoBeneficio(String comercioId, String beneficioId,
                                                      CambiarEstadoBeneficioRequest request) {
        Beneficio beneficio = buscarPropioOFallar(comercioId, beneficioId);

        beneficio.setEstado(request.nuevoEstado());
        // Este cambio SIEMPRE es a propósito (lo dispara el comercio, nunca el job diario):
        // marca la pausa como manual si lo desactiva, y limpia la marca si lo reactiva.
        beneficio.setPausadoManualmente(request.nuevoEstado() == EstadoBeneficio.INACTIVO);
        beneficio.setFechaActualizacion(Instant.now());
        beneficioRepository.save(beneficio);

        log.info("Comercio id={} cambió el beneficio id={} a estado={}", comercioId, beneficioId, request.nuevoEstado());

        return BeneficioResponse.from(beneficio, usosEsteMes(beneficioId));
    }

    /** Solo un tipo que exista Y esté activo es elegible para un beneficio nuevo o editado. */
    private TipoBeneficioCatalogo resolverTipoBeneficioActivo(String tipoBeneficioId) {
        return tipoBeneficioCatalogoRepository.findById(tipoBeneficioId)
                .filter(TipoBeneficioCatalogo::isActivo)
                .orElseThrow(() -> new TipoBeneficioInvalidoException(tipoBeneficioId));
    }

    private long usosEsteMes(String beneficioId) {
        return historialBeneficioRepository.countByBeneficioIdAndFechaUsoAfter(beneficioId, FechaUtil.inicioDeMesActual());
    }

    @Override
    public ValidarBeneficioResponse validarYUsarBeneficio(String comercioId, String usuarioComercioId,
                                                            ValidarBeneficioRequest request) {
        // El token del QR (documento 15.1: "no debe ser una imagen fija permanente")
        // vence a los pocos segundos; si es válido, devuelve el id del socio dueño.
        String socioId = qrTokenService.extraerSocioId(request.codigoQr());
        Socio socio = socioRepository.findById(socioId)
                .orElseThrow(CodigoQrInvalidoException::new);

        // Documento 15.2/15.6: primero se verifica el estado del socio (activo,
        // cuota al día, cuenta no suspendida) y recién después la promoción elegida.
        estadoQrService.validarQrActivo(socio);

        Beneficio beneficio = buscarPropioOFallar(comercioId, request.beneficioId());

        if (!beneficio.estaVigenteHoy()) {
            throw new BeneficioNoVigenteException(beneficio.getId());
        }

        if (historialBeneficioRepository.existsBySocioIdAndBeneficioId(socio.getId(), beneficio.getId())) {
            throw new BeneficioYaCanjeadoException(beneficio.getTitulo());
        }

        HistorialBeneficio historial = HistorialBeneficio.builder()
                .beneficioId(beneficio.getId())
                .beneficioTitulo(beneficio.getTitulo())
                .tipoBeneficioNombre(beneficio.getTipoBeneficioNombre())
                .valor(beneficio.getValor())
                .comercioId(beneficio.getComercioId())
                .comercioNombre(beneficio.getComercioNombre())
                .usuarioComercioId(usuarioComercioId)
                .socioId(socio.getId())
                .socioNumeroSocio(socio.getNumeroSocio())
                .socioNombre(socio.nombreParaMostrar())
                .socioCategoria(socio.getCategoria())
                .montoAhorro(request.montoAhorro())
                .estado(EstadoUsoBeneficio.USADO)
                .fechaUso(Instant.now())
                .build();
        historialBeneficioRepository.save(historial);

        log.info("Comercio id={} aplicó el beneficio id={} al socio id={}", comercioId, beneficio.getId(), socio.getId());

        return ValidarBeneficioResponse.from(historial);
    }

    @Override
    public List<BeneficioResumenResponse> listarBeneficiosVigentes(String rubro, String busqueda) {
        return beneficioRepository.findByEstado(EstadoBeneficio.ACTIVO).stream()
                .filter(Beneficio::estaVigenteHoy)
                .filter(b -> coincideRubro(b.getComercioRubro(), rubro))
                .filter(b -> coincideBusqueda(b, busqueda))
                .map(BeneficioResumenResponse::from)
                .toList();
    }

    @Override
    public List<ComercioConBeneficiosResponse> listarComerciosConBeneficios(String rubro, String busqueda) {
        List<Beneficio> vigentes = beneficioRepository.findByEstado(EstadoBeneficio.ACTIVO).stream()
                .filter(Beneficio::estaVigenteHoy)
                .filter(b -> coincideRubro(b.getComercioRubro(), rubro))
                .filter(b -> coincideBusqueda(b, busqueda))
                .toList();

        return vigentes.stream()
                .map(Beneficio::getComercioId)
                .distinct()
                .map(comercioId -> {
                    Comercio comercio = comercioRepository.findById(comercioId).orElse(null);
                    if (comercio == null) {
                        return null;
                    }
                    List<BeneficioResumenResponse> beneficiosDelComercio = vigentes.stream()
                            .filter(b -> b.getComercioId().equals(comercioId))
                            .map(BeneficioResumenResponse::from)
                            .toList();
                    return ComercioConBeneficiosResponse.of(comercio, beneficiosDelComercio);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<HistorialBeneficioResponse> listarHistorialDeSocio(String socioId) {
        return historialBeneficioRepository.findBySocioIdOrderByFechaUsoDesc(socioId).stream()
                .map(HistorialBeneficioResponse::from)
                .toList();
    }

    private boolean coincideRubro(String comercioRubro, String rubroFiltro) {
        return rubroFiltro == null || rubroFiltro.isBlank()
                || (comercioRubro != null && comercioRubro.equalsIgnoreCase(rubroFiltro));
    }

    private boolean coincideBusqueda(Beneficio beneficio, String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return true;
        }
        String texto = busqueda.toLowerCase();
        return (beneficio.getTitulo() != null && beneficio.getTitulo().toLowerCase().contains(texto))
                || (beneficio.getComercioNombre() != null && beneficio.getComercioNombre().toLowerCase().contains(texto));
    }

    private Beneficio buscarPropioOFallar(String comercioId, String beneficioId) {
        Beneficio beneficio = beneficioRepository.findById(beneficioId)
                .orElseThrow(() -> new BeneficioNoEncontradoException(beneficioId));
        if (!beneficio.getComercioId().equals(comercioId)) {
            // No revelamos que el beneficio existe pero es de otro comercio.
            throw new BeneficioNoEncontradoException(beneficioId);
        }
        return beneficio;
    }
}
