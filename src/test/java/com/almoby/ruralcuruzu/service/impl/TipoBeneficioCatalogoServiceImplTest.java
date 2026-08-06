package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.almoby.ruralcuruzu.domain.TipoBeneficioCatalogo;
import com.almoby.ruralcuruzu.dto.request.ActualizarTipoBeneficioRequest;
import com.almoby.ruralcuruzu.dto.request.CrearTipoBeneficioRequest;
import com.almoby.ruralcuruzu.dto.response.TipoBeneficioActualizadoResponse;
import com.almoby.ruralcuruzu.dto.response.TipoBeneficioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.TipoBeneficioResponse;
import com.almoby.ruralcuruzu.exception.TipoBeneficioCodigoDuplicadoException;
import com.almoby.ruralcuruzu.exception.TipoBeneficioEnUsoException;
import com.almoby.ruralcuruzu.exception.TipoBeneficioNoEncontradoException;
import com.almoby.ruralcuruzu.repository.BeneficioRepository;
import com.almoby.ruralcuruzu.repository.TipoBeneficioCatalogoRepository;

@ExtendWith(MockitoExtension.class)
class TipoBeneficioCatalogoServiceImplTest {

    @Mock
    private TipoBeneficioCatalogoRepository tipoBeneficioCatalogoRepository;
    @Mock
    private BeneficioRepository beneficioRepository;

    private TipoBeneficioCatalogoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TipoBeneficioCatalogoServiceImpl(tipoBeneficioCatalogoRepository, beneficioRepository);
    }

    private TipoBeneficioCatalogo tipo(String id, String codigo, String nombre, boolean activo) {
        return TipoBeneficioCatalogo.builder()
                .id(id)
                .codigo(codigo)
                .nombre(nombre)
                .activo(activo)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();
    }

    // ---------- listarTodos / listarActivos ----------

    @Test
    void listarTodos_devuelveActivosEInactivos() {
        when(tipoBeneficioCatalogoRepository.findAll()).thenReturn(List.of(
                tipo("t1", "DESCUENTO_PORCENTAJE", "Descuento por porcentaje", true),
                tipo("t2", "VIEJO", "Viejo", false)));

        List<TipoBeneficioResponse> resultado = service.listarTodos();

        assertThat(resultado).hasSize(2);
    }

    @Test
    void listarActivos_soloTraeLosActivos() {
        when(tipoBeneficioCatalogoRepository.findByActivoTrue()).thenReturn(List.of(
                tipo("t1", "DESCUENTO_PORCENTAJE", "Descuento por porcentaje", true)));

        List<TipoBeneficioResponse> resultado = service.listarActivos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).activo()).isTrue();
    }

    // ---------- obtenerPorId ----------

    @Test
    void obtenerPorId_existente_devuelveElTipo() {
        when(tipoBeneficioCatalogoRepository.findById("t1"))
                .thenReturn(Optional.of(tipo("t1", "GRATIS", "Gratis", true)));

        TipoBeneficioResponse resultado = service.obtenerPorId("t1");

        assertThat(resultado.codigo()).isEqualTo("GRATIS");
    }

    @Test
    void obtenerPorId_inexistente_lanzaExcepcion() {
        when(tipoBeneficioCatalogoRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId("no-existe"))
                .isInstanceOf(TipoBeneficioNoEncontradoException.class)
                .hasMessageContaining("no-existe");
    }

    // ---------- crear ----------

    @Test
    void crear_codigoLibre_loCreaActivo() {
        when(tipoBeneficioCatalogoRepository.existsByCodigo("DOS_POR_UNO")).thenReturn(false);
        when(tipoBeneficioCatalogoRepository.save(any(TipoBeneficioCatalogo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CrearTipoBeneficioRequest request = new CrearTipoBeneficioRequest("DOS_POR_UNO", "2x1");

        TipoBeneficioCreadoResponse response = service.crear(request);

        assertThat(response.mensaje()).isEqualTo("Tipo de beneficio creado con éxito");
        assertThat(response.tipoBeneficio().codigo()).isEqualTo("DOS_POR_UNO");
        assertThat(response.tipoBeneficio().nombre()).isEqualTo("2x1");
        assertThat(response.tipoBeneficio().activo()).isTrue();
    }

    @Test
    void crear_codigoYaExistente_lanzaExcepcionYNoGuarda() {
        when(tipoBeneficioCatalogoRepository.existsByCodigo("GRATIS")).thenReturn(true);

        CrearTipoBeneficioRequest request = new CrearTipoBeneficioRequest("GRATIS", "Gratis");

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(TipoBeneficioCodigoDuplicadoException.class)
                .hasMessageContaining("GRATIS");
        verify(tipoBeneficioCatalogoRepository, never()).save(any(TipoBeneficioCatalogo.class));
    }

    // ---------- actualizar ----------

    @Test
    void actualizar_soloNombre_dejaActivoSinTocar() {
        TipoBeneficioCatalogo existente = tipo("t1", "GRATIS", "Gratis", true);
        when(tipoBeneficioCatalogoRepository.findById("t1")).thenReturn(Optional.of(existente));
        when(tipoBeneficioCatalogoRepository.save(any(TipoBeneficioCatalogo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TipoBeneficioActualizadoResponse response = service.actualizar(
                "t1", new ActualizarTipoBeneficioRequest("Gratis total", null));

        assertThat(response.tipoBeneficio().nombre()).isEqualTo("Gratis total");
        assertThat(response.tipoBeneficio().activo()).isTrue();
    }

    @Test
    void actualizar_soloActivo_dejaNombreSinTocar() {
        TipoBeneficioCatalogo existente = tipo("t1", "GRATIS", "Gratis", true);
        when(tipoBeneficioCatalogoRepository.findById("t1")).thenReturn(Optional.of(existente));
        when(tipoBeneficioCatalogoRepository.save(any(TipoBeneficioCatalogo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TipoBeneficioActualizadoResponse response = service.actualizar(
                "t1", new ActualizarTipoBeneficioRequest(null, false));

        assertThat(response.tipoBeneficio().nombre()).isEqualTo("Gratis");
        assertThat(response.tipoBeneficio().activo()).isFalse();
    }

    @Test
    void actualizar_inexistente_lanzaExcepcion() {
        when(tipoBeneficioCatalogoRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar("no-existe", new ActualizarTipoBeneficioRequest("X", null)))
                .isInstanceOf(TipoBeneficioNoEncontradoException.class);
    }

    // ---------- eliminar ----------

    @Test
    void eliminar_sinBeneficiosQueLoUsen_loBorra() {
        TipoBeneficioCatalogo existente = tipo("t1", "GRATIS", "Gratis", true);
        when(tipoBeneficioCatalogoRepository.findById("t1")).thenReturn(Optional.of(existente));
        when(beneficioRepository.existsByTipoBeneficioId("t1")).thenReturn(false);

        service.eliminar("t1");

        ArgumentCaptor<TipoBeneficioCatalogo> captor = ArgumentCaptor.forClass(TipoBeneficioCatalogo.class);
        verify(tipoBeneficioCatalogoRepository).delete(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("t1");
    }

    @Test
    void eliminar_conBeneficiosQueLoUsan_lanzaExcepcionYNoBorra() {
        TipoBeneficioCatalogo existente = tipo("t1", "GRATIS", "Gratis", true);
        when(tipoBeneficioCatalogoRepository.findById("t1")).thenReturn(Optional.of(existente));
        when(beneficioRepository.existsByTipoBeneficioId("t1")).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar("t1"))
                .isInstanceOf(TipoBeneficioEnUsoException.class);
        verify(tipoBeneficioCatalogoRepository, never()).delete(any(TipoBeneficioCatalogo.class));
    }

    @Test
    void eliminar_inexistente_lanzaExcepcion() {
        when(tipoBeneficioCatalogoRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar("no-existe"))
                .isInstanceOf(TipoBeneficioNoEncontradoException.class);
        verify(beneficioRepository, never()).existsByTipoBeneficioId(any());
    }
}
