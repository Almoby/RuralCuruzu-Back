package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.almoby.ruralcuruzu.domain.DatosBancarios;
import com.almoby.ruralcuruzu.dto.request.ActualizarDatosBancariosRequest;
import com.almoby.ruralcuruzu.dto.response.DatosBancariosActualizadosResponse;
import com.almoby.ruralcuruzu.dto.response.DatosBancariosResponse;
import com.almoby.ruralcuruzu.exception.DatosBancariosNoConfiguradosException;
import com.almoby.ruralcuruzu.repository.DatosBancariosRepository;

@ExtendWith(MockitoExtension.class)
class DatosBancariosServiceImplTest {

    @Mock
    private DatosBancariosRepository datosBancariosRepository;

    private DatosBancariosServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DatosBancariosServiceImpl(datosBancariosRepository);
    }

    @Test
    void obtener_conDatosCargados_losDevuelve() {
        DatosBancarios datos = DatosBancarios.builder()
                .id("datos-1").banco("Banco Nación").cbu("0000003100000000000001").alias("RURAL.CURUZU")
                .titular("Cooperativa Rural Curuzú").cuit("30-12345678-9").build();
        when(datosBancariosRepository.findAll()).thenReturn(List.of(datos));

        DatosBancariosResponse response = service.obtener();

        assertThat(response.banco()).isEqualTo("Banco Nación");
        assertThat(response.alias()).isEqualTo("RURAL.CURUZU");
    }

    @Test
    void obtener_sinDatosCargados_lanzaExcepcion() {
        when(datosBancariosRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.obtener())
                .isInstanceOf(DatosBancariosNoConfiguradosException.class);
    }

    @Test
    void actualizar_sinDatosPrevios_losCreaYLoInformaEnElMensaje() {
        when(datosBancariosRepository.findAll()).thenReturn(List.of());
        when(datosBancariosRepository.save(any(DatosBancarios.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarDatosBancariosRequest request = new ActualizarDatosBancariosRequest(
                "Banco Nación", "0000003100000000000001", "RURAL.CURUZU", "Cooperativa Rural Curuzú", "30-12345678-9");

        DatosBancariosActualizadosResponse response = service.actualizar(request);

        assertThat(response.mensaje()).isEqualTo("Datos bancarios creados con éxito");
        assertThat(response.datosBancarios().banco()).isEqualTo("Banco Nación");

        ArgumentCaptor<DatosBancarios> captor = ArgumentCaptor.forClass(DatosBancarios.class);
        verify(datosBancariosRepository).save(captor.capture());
        assertThat(captor.getValue().getCbu()).isEqualTo("0000003100000000000001");
    }

    @Test
    void actualizar_conDatosPrevios_losReemplazaYLoInformaEnElMensaje() {
        DatosBancarios existentes = DatosBancarios.builder()
                .id("datos-1").banco("Banco Viejo").cbu("111").alias("VIEJO").titular("X").cuit("Y").build();
        when(datosBancariosRepository.findAll()).thenReturn(List.of(existentes));
        when(datosBancariosRepository.save(any(DatosBancarios.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarDatosBancariosRequest request = new ActualizarDatosBancariosRequest(
                "Banco Nuevo", "222", "NUEVO", "Titular Nuevo", "CUIT Nuevo");

        DatosBancariosActualizadosResponse response = service.actualizar(request);

        assertThat(response.mensaje()).isEqualTo("Datos bancarios actualizados con éxito");
        assertThat(response.datosBancarios().banco()).isEqualTo("Banco Nuevo");
        assertThat(response.datosBancarios().alias()).isEqualTo("NUEVO");

        ArgumentCaptor<DatosBancarios> captor = ArgumentCaptor.forClass(DatosBancarios.class);
        verify(datosBancariosRepository).save(captor.capture());
        // Sigue siendo el mismo documento (mismo id), no uno nuevo: es upsert, no alta.
        assertThat(captor.getValue().getId()).isEqualTo("datos-1");
    }
}
