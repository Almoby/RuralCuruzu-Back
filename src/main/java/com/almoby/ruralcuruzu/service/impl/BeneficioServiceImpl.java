package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.domain.Comercio;
import com.almoby.ruralcuruzu.domain.HistorialBeneficio;
import com.almoby.ruralcuruzu.domain.Socio;
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
import com.almoby.ruralcuruzu.repository.BeneficioRepository;
import com.almoby.ruralcuruzu.repository.ComercioRepository;
import com.almoby.ruralcuruzu.repository.HistorialBeneficioRepository;
import com.almoby.ruralcuruzu.repository.SocioRepository;
import com.almoby.ruralcuruzu.security.jwt.QrTokenService;
import com.almoby.ruralcuruzu.service.BeneficioService;
import com.almoby.ruralcuruzu.service.EstadoQrService;

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

    public BeneficioServiceImpl(BeneficioRepository beneficioRepository,
                                 HistorialBeneficioRepository historialBeneficioRepository,
                                 ComercioRepository comercioRepository,
                                 SocioRepository socioRepository,
                                 EstadoQrService estadoQrService,
                                 QrTokenService qrTokenService) {
        this.beneficioRepository = beneficioRepository;
        this.historialBeneficioRepository = historialBeneficioRepository;
        this.comercioRepository = comercioRepository;
        this.socioRepository = socioRepository;
        this.estadoQrService = estadoQrService;
        this.qrTokenService = qrTokenService;
    }

    @Override
    public BeneficioCreadoResponse crearBeneficio(String comercioId, CrearBeneficioRequest request) {
        Comercio comercio = comercioRepository.findById(comercioId)
                .orElseThrow(() -> new ComercioNoEncontradoException(comercioId));

        Instant ahora = Instant.now();
        Beneficio beneficio = Beneficio.builder()
                .comercioId(comercio.getId())
                .comercioNombre(comercio.getNombreComercial())
                .comercioRubro(comercio.getRubro())
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .tipo(request.tipo())
                .valor(request.valor())
                .fechaInicioVigencia(request.fechaInicioVigencia())
                .fechaFinVigencia(request.fechaFinVigencia())
                .estado(EstadoBeneficio.ACTIVO)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .build();
        beneficioRepository.save(beneficio);

        log.info("Comercio id={} creó el beneficio id={} ({})", comercioId, beneficio.getId(), request.titulo());

        return BeneficioCreadoResponse.of(BeneficioResponse.from(beneficio));
    }

    @Override
    public List<BeneficioResponse> listarBeneficiosDelComercio(String comercioId) {
        return beneficioRepository.findByComercioId(comercioId).stream()
                .map(BeneficioResponse::from)
                .toList();
    }

    @Override
    public BeneficioResponse obtenerBeneficioDelComercio(String comercioId, String beneficioId) {
        return BeneficioResponse.from(buscarPropioOFallar(comercioId, beneficioId));
    }

    @Override
    public BeneficioResponse actualizarBeneficio(String comercioId, String beneficioId,
                                                  ActualizarBeneficioRequest request) {
        Beneficio beneficio = buscarPropioOFallar(comercioId, beneficioId);

        beneficio.setTitulo(request.titulo());
        beneficio.setDescripcion(request.descripcion());
        beneficio.setTipo(request.tipo());
        beneficio.setValor(request.valor());
        beneficio.setFechaInicioVigencia(request.fechaInicioVigencia());
        beneficio.setFechaFinVigencia(request.fechaFinVigencia());
        beneficio.setFechaActualizacion(Instant.now());
        beneficioRepository.save(beneficio);

        log.info("Comercio id={} actualizó el beneficio id={}", comercioId, beneficioId);

        return BeneficioResponse.from(beneficio);
    }

    @Override
    public BeneficioResponse cambiarEstadoBeneficio(String comercioId, String beneficioId,
                                                      CambiarEstadoBeneficioRequest request) {
        Beneficio beneficio = buscarPropioOFallar(comercioId, beneficioId);

        beneficio.setEstado(request.nuevoEstado());
        beneficio.setFechaActualizacion(Instant.now());
        beneficioRepository.save(beneficio);

        log.info("Comercio id={} cambió el beneficio id={} a estado={}", comercioId, beneficioId, request.nuevoEstado());

        return BeneficioResponse.from(beneficio);
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
                .tipo(beneficio.getTipo())
                .valor(beneficio.getValor())
                .comercioId(beneficio.getComercioId())
                .comercioNombre(beneficio.getComercioNombre())
                .usuarioComercioId(usuarioComercioId)
                .socioId(socio.getId())
                .socioNumeroSocio(socio.getNumeroSocio())
                .socioNombre(socio.nombreParaMostrar())
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
