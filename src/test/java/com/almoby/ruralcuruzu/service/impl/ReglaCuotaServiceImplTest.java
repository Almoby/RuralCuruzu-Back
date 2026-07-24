package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.almoby.ruralcuruzu.domain.ReglaCuota;
import com.almoby.ruralcuruzu.dto.request.ActualizarReglaCuotaRequest;
import com.almoby.ruralcuruzu.dto.response.ReglaCuotaActualizadaResponse;
import com.almoby.ruralcuruzu.dto.response.ReglaCuotaResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.exception.ReglaCuotaNoEncontradaException;
import com.almoby.ruralcuruzu.repository.ReglaCuotaRepository;

@ExtendWith(MockitoExtension.class)
class ReglaCuotaServiceImplTest {

    @Mock
    private ReglaCuotaRepository reglaCuotaRepository;

    private ReglaCuotaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReglaCuotaServiceImpl(reglaCuotaRepository);
    }

    private ReglaCuota reglaExistente() {
        return ReglaCuota.builder()
                .id("regla-1")
                .categoriaAplicable(CategoriaSocio.ACTIVO)
                .nombre("Cuota de socio activo")
                .importe(new BigDecimal("15000.00"))
                .diaVencimiento(10)
                .build();
    }

    @Test
    void listarReglas_devuelveTodasLasCargadas() {
        when(reglaCuotaRepository.findAll()).thenReturn(List.of(reglaExistente()));

        List<ReglaCuotaResponse> resultado = service.listarReglas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).categoriaAplicable()).isEqualTo(CategoriaSocio.ACTIVO);
    }

    @Test
    void obtenerPorCategoria_existente_devuelveLaRegla() {
        when(reglaCuotaRepository.findByCategoriaAplicable(CategoriaSocio.ACTIVO))
                .thenReturn(Optional.of(reglaExistente()));

        ReglaCuotaResponse resultado = service.obtenerPorCategoria(CategoriaSocio.ACTIVO);

        assertThat(resultado.importe()).isEqualByComparingTo("15000.00");
        assertThat(resultado.diaVencimiento()).isEqualTo(10);
    }

    @Test
    void obtenerPorCategoria_inexistente_lanzaExcepcion() {
        when(reglaCuotaRepository.findByCategoriaAplicable(CategoriaSocio.ADHERENTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorCategoria(CategoriaSocio.ADHERENTE))
                .isInstanceOf(ReglaCuotaNoEncontradaException.class);
    }

    @Test
    void actualizarRegla_sinRegistroPrevio_laCreaYMensajeDiceCreada() {
        when(reglaCuotaRepository.findByCategoriaAplicable(CategoriaSocio.ADHERENTE)).thenReturn(Optional.empty());
        when(reglaCuotaRepository.save(any(ReglaCuota.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarReglaCuotaRequest request = new ActualizarReglaCuotaRequest(
                "Cuota de socio adherente", new BigDecimal("8000.00"), 15);

        ReglaCuotaActualizadaResponse resultado = service.actualizarRegla(CategoriaSocio.ADHERENTE, request);

        assertThat(resultado.mensaje()).isEqualTo("Regla de cuota creada con éxito");
        assertThat(resultado.regla().categoriaAplicable()).isEqualTo(CategoriaSocio.ADHERENTE);
        assertThat(resultado.regla().importe()).isEqualByComparingTo("8000.00");
        assertThat(resultado.regla().diaVencimiento()).isEqualTo(15);
    }

    @Test
    void actualizarRegla_conRegistroPrevio_loActualizaEnVezDeDuplicarYMensajeDiceActualizada() {
        ReglaCuota existente = reglaExistente();
        when(reglaCuotaRepository.findByCategoriaAplicable(CategoriaSocio.ACTIVO)).thenReturn(Optional.of(existente));
        when(reglaCuotaRepository.save(any(ReglaCuota.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarReglaCuotaRequest request = new ActualizarReglaCuotaRequest(
                "Cuota de socio activo", new BigDecimal("18000.00"), 5);

        ReglaCuotaActualizadaResponse resultado = service.actualizarRegla(CategoriaSocio.ACTIVO, request);

        ArgumentCaptor<ReglaCuota> captor = ArgumentCaptor.forClass(ReglaCuota.class);
        verify(reglaCuotaRepository).save(captor.capture());

        assertThat(captor.getValue().getId()).isEqualTo("regla-1");
        assertThat(resultado.mensaje()).isEqualTo("Regla de cuota actualizada con éxito");
        assertThat(resultado.regla().importe()).isEqualByComparingTo("18000.00");
        assertThat(resultado.regla().diaVencimiento()).isEqualTo(5);
    }
}
