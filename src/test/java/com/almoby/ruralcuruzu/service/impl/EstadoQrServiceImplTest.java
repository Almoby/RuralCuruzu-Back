package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.almoby.ruralcuruzu.domain.Cuota;
import com.almoby.ruralcuruzu.domain.DatosPago;
import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.dto.response.EstadoQrResponse;
import com.almoby.ruralcuruzu.enums.EstadoCuota;
import com.almoby.ruralcuruzu.enums.EstadoQr;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.EstadoUsuario;
import com.almoby.ruralcuruzu.exception.QrNoValidoException;
import com.almoby.ruralcuruzu.repository.CuotaRepository;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;

/**
 * Ver documento, secciones 15.2 y 15.3: estos tests cubren todas las
 * combinaciones de estado que determinan si el QR de un socio está ACTIVO.
 */
@ExtendWith(MockitoExtension.class)
class EstadoQrServiceImplTest {

    @Mock
    private CuotaRepository cuotaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private EstadoQrServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EstadoQrServiceImpl(cuotaRepository, usuarioRepository);
    }

    private Socio socio(EstadoSocio estado, String usuarioId) {
        return Socio.builder().id("socio-1").numeroSocio("SOC-000001").estado(estado).usuarioId(usuarioId).build();
    }

    private Usuario usuario(EstadoUsuario estado) {
        return Usuario.builder().id("usuario-1").estado(estado).build();
    }

    private Cuota cuota(String periodo, EstadoCuota estado, LocalDate fechaVencimiento) {
        return Cuota.builder().socioId("socio-1").periodo(periodo).estado(estado)
                .fechaVencimiento(fechaVencimiento).build();
    }

    @Test
    void calcularEstado_todoEnRegla_devuelveActivo() {
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO)));
        when(cuotaRepository.findBySocioId("socio-1")).thenReturn(List.of(
                cuota("2026-06", EstadoCuota.PENDIENTE, LocalDate.of(2026, 6, 9))));

        EstadoQrResponse resultado = service.calcularEstado(socio(EstadoSocio.ACTIVO, "usuario-1"));

        assertThat(resultado.estado()).isEqualTo(EstadoQr.ACTIVO);
    }

    @Test
    void calcularEstado_usuarioBloqueado_devuelveBloqueado_conPrioridadSobreTodoLoDemas() {
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(usuario(EstadoUsuario.BLOQUEADO)));

        EstadoQrResponse resultado = service.calcularEstado(socio(EstadoSocio.ACTIVO, "usuario-1"));

        assertThat(resultado.estado()).isEqualTo(EstadoQr.BLOQUEADO);
    }

    @Test
    void calcularEstado_socioNoActivo_devuelveVencido() {
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO)));

        EstadoQrResponse resultado = service.calcularEstado(socio(EstadoSocio.DADO_DE_BAJA, "usuario-1"));

        assertThat(resultado.estado()).isEqualTo(EstadoQr.VENCIDO);
    }

    @Test
    void calcularEstado_usuarioInactivoSinEstarBloqueado_devuelveInactivoPorSuspension() {
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(usuario(EstadoUsuario.INACTIVO)));

        EstadoQrResponse resultado = service.calcularEstado(socio(EstadoSocio.ACTIVO, "usuario-1"));

        assertThat(resultado.estado()).isEqualTo(EstadoQr.INACTIVO_POR_SUSPENSION);
    }

    @Test
    void calcularEstado_conCuotaVencida_devuelveInactivoPorDeuda() {
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO)));
        when(cuotaRepository.findBySocioId("socio-1")).thenReturn(List.of(
                cuota("2026-05", EstadoCuota.VENCIDA, LocalDate.of(2026, 5, 9))));

        EstadoQrResponse resultado = service.calcularEstado(socio(EstadoSocio.ACTIVO, "usuario-1"));

        assertThat(resultado.estado()).isEqualTo(EstadoQr.INACTIVO_POR_DEUDA);
    }

    @Test
    void calcularEstado_sinUsuarioVinculado_noRompeYSoloMiraSocioYCuotas() {

        EstadoQrResponse resultado = service.calcularEstado(socio(EstadoSocio.ACTIVO, null));

        assertThat(resultado.estado()).isEqualTo(EstadoQr.ACTIVO);
    }

    @Test
    void calcularEstado_fechaValidezEsElVencimientoDelPeriodoMasReciente() {
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO)));
        when(cuotaRepository.findBySocioId("socio-1")).thenReturn(List.of(
                cuota("2026-04", EstadoCuota.PAGADA, LocalDate.of(2026, 4, 9)),
                cuota("2026-06", EstadoCuota.PENDIENTE, LocalDate.of(2026, 6, 9)),
                cuota("2026-05", EstadoCuota.PAGADA, LocalDate.of(2026, 5, 9))));

        EstadoQrResponse resultado = service.calcularEstado(socio(EstadoSocio.ACTIVO, "usuario-1"));

        assertThat(resultado.fechaValidez()).isEqualTo(LocalDate.of(2026, 6, 9));
    }

    @Test
    void calcularEstado_ultimoPagoEsElDeLaCuotaPagadaMasReciente() {
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO)));
        Cuota abril = cuota("2026-04", EstadoCuota.PAGADA, LocalDate.of(2026, 4, 9));
        abril.setDatosPago(DatosPago.builder().fechaPago(Instant.parse("2026-04-08T12:00:00Z"))
                .importe(BigDecimal.TEN).build());
        Cuota mayo = cuota("2026-05", EstadoCuota.PAGADA, LocalDate.of(2026, 5, 9));
        mayo.setDatosPago(DatosPago.builder().fechaPago(Instant.parse("2026-05-08T12:00:00Z"))
                .importe(BigDecimal.TEN).build());
        Cuota junio = cuota("2026-06", EstadoCuota.PENDIENTE, LocalDate.of(2026, 6, 9)); // todavía sin pagar

        when(cuotaRepository.findBySocioId("socio-1")).thenReturn(List.of(abril, mayo, junio));

        EstadoQrResponse resultado = service.calcularEstado(socio(EstadoSocio.ACTIVO, "usuario-1"));

        assertThat(resultado.ultimoPago()).isEqualTo(Instant.parse("2026-05-08T12:00:00Z"));
    }

    @Test
    void validarQrActivo_conEstadoNoActivo_lanzaExcepcionConElMensajeDelEstado() {
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO)));
        when(cuotaRepository.findBySocioId("socio-1")).thenReturn(List.of(
                cuota("2026-05", EstadoCuota.VENCIDA, LocalDate.of(2026, 5, 9))));

        assertThatThrownBy(() -> service.validarQrActivo(socio(EstadoSocio.ACTIVO, "usuario-1")))
                .isInstanceOf(QrNoValidoException.class)
                .hasMessage("Tenés cuotas vencidas. Regularizá tu situación para volver a usar el QR.");
    }

    @Test
    void validarQrActivo_conEstadoActivo_noLanzaNada() {
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO)));
        when(cuotaRepository.findBySocioId("socio-1")).thenReturn(List.of());

        service.validarQrActivo(socio(EstadoSocio.ACTIVO, "usuario-1"));
    }
}
