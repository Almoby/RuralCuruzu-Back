package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.almoby.ruralcuruzu.domain.TipoCuota;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoTipoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.CrearTipoCuotaRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoTipoCuotaResponse;
import com.almoby.ruralcuruzu.dto.response.TipoCuotaCreadoResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoTipoCuota;
import com.almoby.ruralcuruzu.exception.TipoCuotaNoEncontradoException;
import com.almoby.ruralcuruzu.repository.TipoCuotaRepository;

@ExtendWith(MockitoExtension.class)
class TipoCuotaServiceImplTest {

    @Mock
    private TipoCuotaRepository tipoCuotaRepository;

    private TipoCuotaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TipoCuotaServiceImpl(tipoCuotaRepository);
    }

    private CrearTipoCuotaRequest requestValido() {
        return new CrearTipoCuotaRequest(
                "Cuota de socio activo", "Cuota mensual", CategoriaSocio.ACTIVO,
                new BigDecimal("15000.00"), LocalDate.of(2026, 8, 1), 10, null);
    }

    @Test
    void crearTipoCuota_conDatosValidos_quedaActivoPorDefecto() {
        when(tipoCuotaRepository.save(any(TipoCuota.class))).thenAnswer(invocation -> {
            TipoCuota tipoCuota = invocation.getArgument(0);
            tipoCuota.setId("tipo-1");
            return tipoCuota;
        });

        TipoCuotaCreadoResponse response = service.crearTipoCuota(requestValido());

        assertThat(response.mensaje()).isEqualTo("Tipo de cuota creado con éxito");
        assertThat(response.tipoCuota().nombre()).isEqualTo("Cuota de socio activo");
        assertThat(response.tipoCuota().estado()).isEqualTo(EstadoTipoCuota.ACTIVO);
        assertThat(response.tipoCuota().categoriaAplicable()).isEqualTo(CategoriaSocio.ACTIVO);
    }

    @Test
    void crearTipoCuota_conEstadoEspecificado_respetaEseEstado() {
        CrearTipoCuotaRequest request = new CrearTipoCuotaRequest(
                "Cuota extraordinaria", null, CategoriaSocio.ADHERENTE,
                new BigDecimal("5000.00"), LocalDate.of(2026, 8, 1), 15, EstadoTipoCuota.INACTIVO);
        when(tipoCuotaRepository.save(any(TipoCuota.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TipoCuotaCreadoResponse response = service.crearTipoCuota(request);

        assertThat(response.tipoCuota().estado()).isEqualTo(EstadoTipoCuota.INACTIVO);
    }

    @Test
    void obtenerTipoCuotaPorId_inexistente_lanzaExcepcion() {
        when(tipoCuotaRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerTipoCuotaPorId("no-existe"))
                .isInstanceOf(TipoCuotaNoEncontradoException.class);
    }

    @Test
    void cambiarEstadoTipoCuota_actualizaYDevuelveMensaje() {
        TipoCuota tipoCuota = TipoCuota.builder().id("tipo-1").estado(EstadoTipoCuota.ACTIVO).build();
        when(tipoCuotaRepository.findById("tipo-1")).thenReturn(Optional.of(tipoCuota));

        CambiarEstadoTipoCuotaResponse response = service.cambiarEstadoTipoCuota(
                "tipo-1", new CambiarEstadoTipoCuotaRequest(EstadoTipoCuota.INACTIVO));

        assertThat(response.estado()).isEqualTo(EstadoTipoCuota.INACTIVO);
        assertThat(response.mensaje()).isEqualTo("Tipo de cuota desactivado correctamente");
        assertThat(tipoCuota.getEstado()).isEqualTo(EstadoTipoCuota.INACTIVO);
    }

    @Test
    void cambiarEstadoTipoCuota_inexistente_lanzaExcepcion() {
        when(tipoCuotaRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstadoTipoCuota(
                "no-existe", new CambiarEstadoTipoCuotaRequest(EstadoTipoCuota.INACTIVO)))
                .isInstanceOf(TipoCuotaNoEncontradoException.class);
    }

    @Test
    void listarTiposCuota_sinFiltro_usaFindAll() {
        when(tipoCuotaRepository.findAll()).thenReturn(java.util.List.of(
                TipoCuota.builder().id("tipo-1").estado(EstadoTipoCuota.ACTIVO).build()));

        java.util.List<?> resultado = service.listarTiposCuota(null);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void listarTiposCuota_conFiltro_usaFindByEstado() {
        when(tipoCuotaRepository.findByEstado(EstadoTipoCuota.ACTIVO)).thenReturn(java.util.List.of(
                TipoCuota.builder().id("tipo-1").estado(EstadoTipoCuota.ACTIVO).build()));

        java.util.List<?> resultado = service.listarTiposCuota(EstadoTipoCuota.ACTIVO);

        assertThat(resultado).hasSize(1);
    }
}
