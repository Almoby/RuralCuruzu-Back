package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.Cuota;
import com.almoby.ruralcuruzu.domain.Pago;
import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.dto.response.EstadoQrResponse;
import com.almoby.ruralcuruzu.enums.EstadoCuota;
import com.almoby.ruralcuruzu.enums.EstadoPago;
import com.almoby.ruralcuruzu.enums.EstadoQr;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.EstadoUsuario;
import com.almoby.ruralcuruzu.exception.QrNoValidoException;
import com.almoby.ruralcuruzu.repository.CuotaRepository;
import com.almoby.ruralcuruzu.repository.PagoRepository;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.service.EstadoQrService;

/**
 * Ver documento, secciones 15.2 y 15.3. Orden de prioridad cuando varias
 * condiciones fallan a la vez (ej. un socio dado de baja Y con cuotas
 * vencidas): primero se informa lo más "grave" para la cuenta en sí
 * (bloqueado por el admin, membresía no vigente, cuenta suspendida) y recién
 * al final la deuda, que es la única condición que el socio puede resolver
 * él mismo pagando.
 *
 * "Membresía vigente" y "socio activo" (15.2) son la misma condición acá: no
 * hay ningún campo de vencimiento de membresía separado del estado del socio.
 */
@Service
public class EstadoQrServiceImpl implements EstadoQrService {

    private final CuotaRepository cuotaRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;

    public EstadoQrServiceImpl(CuotaRepository cuotaRepository, PagoRepository pagoRepository,
                                UsuarioRepository usuarioRepository) {
        this.cuotaRepository = cuotaRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public EstadoQrResponse calcularEstado(Socio socio) {
        List<Cuota> cuotas = cuotaRepository.findBySocioId(socio.getId());
        EstadoUsuario estadoUsuario = obtenerEstadoUsuario(socio);

        EstadoQr estado = calcularEstadoQr(socio, estadoUsuario, cuotas);

        return new EstadoQrResponse(estado, mensajePara(estado), fechaValidez(cuotas), ultimoPago(cuotas));
    }

    @Override
    public void validarQrActivo(Socio socio) {
        EstadoQrResponse estadoActual = calcularEstado(socio);
        if (estadoActual.estado() != EstadoQr.ACTIVO) {
            throw new QrNoValidoException(estadoActual.estado(), estadoActual.mensaje());
        }
    }

    private EstadoUsuario obtenerEstadoUsuario(Socio socio) {
        if (socio.getUsuarioId() == null) {
            return null;
        }
        return usuarioRepository.findById(socio.getUsuarioId())
                .map(Usuario::getEstado)
                .orElse(null);
    }

    private EstadoQr calcularEstadoQr(Socio socio, EstadoUsuario estadoUsuario, List<Cuota> cuotas) {
        if (estadoUsuario == EstadoUsuario.BLOQUEADO) {
            return EstadoQr.BLOQUEADO;
        }
        if (socio.getEstado() != EstadoSocio.ACTIVO) {
            return EstadoQr.VENCIDO;
        }
        if (estadoUsuario != null && estadoUsuario != EstadoUsuario.ACTIVO) {
            return EstadoQr.INACTIVO_POR_SUSPENSION;
        }
        boolean tieneCuotaVencida = cuotas.stream().anyMatch(c -> c.getEstado() == EstadoCuota.VENCIDA);
        if (tieneCuotaVencida) {
            return EstadoQr.INACTIVO_POR_DEUDA;
        }
        return EstadoQr.ACTIVO;
    }

    private String mensajePara(EstadoQr estado) {
        return switch (estado) {
            case ACTIVO -> "Tu QR está activo. Podés usarlo en todos los comercios adheridos.";
            case INACTIVO_POR_DEUDA -> "Tenés cuotas vencidas. Regularizá tu situación para volver a usar el QR.";
            case INACTIVO_POR_SUSPENSION -> "Tu cuenta está suspendida. Contactá a la cooperativa para más información.";
            case VENCIDO -> "Tu membresía no está vigente. Contactá a la cooperativa para más información.";
            case BLOQUEADO -> "Tu cuenta fue bloqueada. Contactá a la cooperativa para más información.";
        };
    }

    /** Fecha de vencimiento de la cuota del período más reciente (equivalente a "próximo vencimiento"). */
    private LocalDate fechaValidez(List<Cuota> cuotas) {
        return cuotas.stream()
                .max(Comparator.comparing(Cuota::getPeriodo))
                .map(Cuota::getFechaVencimiento)
                .orElse(null);
    }

    /** Fecha del último pago acreditado (el Pago APROBADO de la cuota PAGADA más reciente), si tiene alguno. */
    private Instant ultimoPago(List<Cuota> cuotas) {
        return cuotas.stream()
                .filter(c -> c.getEstado() == EstadoCuota.PAGADA)
                .max(Comparator.comparing(Cuota::getPeriodo))
                .flatMap(c -> pagoRepository.findByCuotaIdAndEstado(c.getId(), EstadoPago.APROBADO))
                .map(Pago::getFechaPago)
                .orElse(null);
    }
}
