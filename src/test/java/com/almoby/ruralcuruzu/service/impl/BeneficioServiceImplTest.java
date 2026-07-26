package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.domain.Comercio;
import com.almoby.ruralcuruzu.domain.DatosPersonaFisica;
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
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoBeneficio;
import com.almoby.ruralcuruzu.enums.EstadoComercio;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.EstadoUsoBeneficio;
import com.almoby.ruralcuruzu.enums.TipoBeneficio;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.exception.BeneficioNoEncontradoException;
import com.almoby.ruralcuruzu.exception.BeneficioNoVigenteException;
import com.almoby.ruralcuruzu.exception.BeneficioYaCanjeadoException;
import com.almoby.ruralcuruzu.exception.CodigoQrInvalidoException;
import com.almoby.ruralcuruzu.exception.ComercioNoEncontradoException;
import com.almoby.ruralcuruzu.repository.BeneficioRepository;
import com.almoby.ruralcuruzu.repository.ComercioRepository;
import com.almoby.ruralcuruzu.repository.HistorialBeneficioRepository;
import com.almoby.ruralcuruzu.repository.SocioRepository;

@ExtendWith(MockitoExtension.class)
class BeneficioServiceImplTest {

    @Mock
    private BeneficioRepository beneficioRepository;
    @Mock
    private HistorialBeneficioRepository historialBeneficioRepository;
    @Mock
    private ComercioRepository comercioRepository;
    @Mock
    private SocioRepository socioRepository;

    private BeneficioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BeneficioServiceImpl(beneficioRepository, historialBeneficioRepository, comercioRepository, socioRepository);
    }

    private Comercio comercio() {
        return Comercio.builder()
                .id("comercio-1")
                .nombreComercial("Farmacia Del Sol")
                .razonSocial("Farmacia Del Sol SRL")
                .cuit("30-12345678-9")
                .rubro("Farmacia")
                .telefono("+54 11 4444-5555")
                .correoElectronico("contacto@farmaciadelsol.com")
                .direccion("Av. Santa Fe 1200, CABA")
                .estado(EstadoComercio.ACTIVO)
                .build();
    }

    private Beneficio beneficioVigente(String id, String comercioId) {
        return Beneficio.builder()
                .id(id)
                .comercioId(comercioId)
                .comercioNombre("Farmacia Del Sol")
                .comercioRubro("Farmacia")
                .titulo("15% en medicamentos")
                .descripcion("Descuento en toda la línea de medicamentos de venta libre")
                .tipo(TipoBeneficio.DESCUENTO_PORCENTAJE)
                .valor("15%")
                .estado(EstadoBeneficio.ACTIVO)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();
    }

    private Socio socio() {
        DatosPersonaFisica datos = new DatosPersonaFisica(
                "Pérez, Juan", "12345678", null, null, null, null, null, "juan@example.com", null, null);
        return Socio.builder()
                .id("socio-1")
                .numeroSocio("SOC-000001")
                .categoria(CategoriaSocio.ACTIVO)
                .tipoPersona(TipoPersona.FISICA)
                .datosPersonaFisica(datos)
                .estado(EstadoSocio.ACTIVO)
                .codigoQr("qr-abc-123")
                .build();
    }

    // ---------- crearBeneficio ----------

    @Test
    void crearBeneficio_conComercioExistente_loCreaActivo() {
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio()));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearBeneficioRequest request = new CrearBeneficioRequest(
                "15% en medicamentos", "Descuento en venta libre", TipoBeneficio.DESCUENTO_PORCENTAJE, "15%",
                null, LocalDate.of(2026, 12, 31));

        BeneficioCreadoResponse response = service.crearBeneficio("comercio-1", request);

        assertThat(response.mensaje()).isEqualTo("Beneficio creado con éxito");
        assertThat(response.beneficio().comercioNombre()).isEqualTo("Farmacia Del Sol");
        assertThat(response.beneficio().comercioId()).isEqualTo("comercio-1");
        assertThat(response.beneficio().estado()).isEqualTo(EstadoBeneficio.ACTIVO);
    }

    @Test
    void crearBeneficio_comercioInexistente_lanzaExcepcion() {
        when(comercioRepository.findById("no-existe")).thenReturn(Optional.empty());

        CrearBeneficioRequest request = new CrearBeneficioRequest(
                "titulo", "desc", TipoBeneficio.GRATIS, "Gratis", null, null);

        assertThatThrownBy(() -> service.crearBeneficio("no-existe", request))
                .isInstanceOf(ComercioNoEncontradoException.class);
    }

    // ---------- listar / obtener / actualizar / cambiar estado (ownership) ----------

    @Test
    void obtenerBeneficioDelComercio_deOtroComercio_lanzaBeneficioNoEncontrado() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));

        assertThatThrownBy(() -> service.obtenerBeneficioDelComercio("comercio-ajeno", "beneficio-1"))
                .isInstanceOf(BeneficioNoEncontradoException.class);
    }

    @Test
    void actualizarBeneficio_propio_loActualiza() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarBeneficioRequest request = new ActualizarBeneficioRequest(
                "20% en medicamentos", "nueva desc", TipoBeneficio.DESCUENTO_PORCENTAJE, "20%", null, null);

        BeneficioResponse response = service.actualizarBeneficio("comercio-1", "beneficio-1", request);

        assertThat(response.titulo()).isEqualTo("20% en medicamentos");
        assertThat(response.valor()).isEqualTo("20%");
    }

    @Test
    void cambiarEstadoBeneficio_propio_loActualiza() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        BeneficioResponse response = service.cambiarEstadoBeneficio(
                "comercio-1", "beneficio-1", new CambiarEstadoBeneficioRequest(EstadoBeneficio.INACTIVO));

        assertThat(response.estado()).isEqualTo(EstadoBeneficio.INACTIVO);
    }

    // ---------- validarYUsarBeneficio ----------

    @Test
    void validarYUsarBeneficio_qrYBeneficioValidos_creaHistorialYDevuelveConfirmacion() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        when(socioRepository.findByCodigoQr("qr-abc-123")).thenReturn(Optional.of(socio()));
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));
        when(historialBeneficioRepository.existsBySocioIdAndBeneficioId("socio-1", "beneficio-1")).thenReturn(false);
        when(historialBeneficioRepository.save(any(HistorialBeneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-abc-123", "beneficio-1", new BigDecimal("450.00"));

        ValidarBeneficioResponse response = service.validarYUsarBeneficio("comercio-1", request);

        assertThat(response.mensaje()).isEqualTo("Beneficio aplicado con éxito");
        assertThat(response.socioNombre()).isEqualTo("Pérez, Juan");
        assertThat(response.beneficioTitulo()).isEqualTo("15% en medicamentos");
        assertThat(response.montoAhorro()).isEqualByComparingTo("450.00");

        ArgumentCaptor<HistorialBeneficio> captor = ArgumentCaptor.forClass(HistorialBeneficio.class);
        verify(historialBeneficioRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoUsoBeneficio.USADO);
        assertThat(captor.getValue().getSocioId()).isEqualTo("socio-1");
    }

    @Test
    void validarYUsarBeneficio_codigoQrInexistente_lanzaExcepcion() {
        when(socioRepository.findByCodigoQr("qr-invalido")).thenReturn(Optional.empty());

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-invalido", "beneficio-1", BigDecimal.TEN);

        assertThatThrownBy(() -> service.validarYUsarBeneficio("comercio-1", request))
                .isInstanceOf(CodigoQrInvalidoException.class);
    }

    @Test
    void validarYUsarBeneficio_beneficioDeOtroComercio_lanzaExcepcion() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-ajeno");
        when(socioRepository.findByCodigoQr("qr-abc-123")).thenReturn(Optional.of(socio()));
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-abc-123", "beneficio-1", BigDecimal.TEN);

        assertThatThrownBy(() -> service.validarYUsarBeneficio("comercio-1", request))
                .isInstanceOf(BeneficioNoEncontradoException.class);
    }

    @Test
    void validarYUsarBeneficio_beneficioPausado_lanzaExcepcion() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        beneficio.setEstado(EstadoBeneficio.INACTIVO);
        when(socioRepository.findByCodigoQr("qr-abc-123")).thenReturn(Optional.of(socio()));
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-abc-123", "beneficio-1", BigDecimal.TEN);

        assertThatThrownBy(() -> service.validarYUsarBeneficio("comercio-1", request))
                .isInstanceOf(BeneficioNoVigenteException.class);
    }

    @Test
    void validarYUsarBeneficio_beneficioVencido_lanzaExcepcion() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        beneficio.setFechaFinVigencia(LocalDate.now().minusDays(1));
        when(socioRepository.findByCodigoQr("qr-abc-123")).thenReturn(Optional.of(socio()));
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-abc-123", "beneficio-1", BigDecimal.TEN);

        assertThatThrownBy(() -> service.validarYUsarBeneficio("comercio-1", request))
                .isInstanceOf(BeneficioNoVigenteException.class);
    }

    @Test
    void validarYUsarBeneficio_socioYaLoHabiaCanjeadoAntes_lanzaExcepcionYNoDuplicaElRegistro() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        when(socioRepository.findByCodigoQr("qr-abc-123")).thenReturn(Optional.of(socio()));
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));
        when(historialBeneficioRepository.existsBySocioIdAndBeneficioId("socio-1", "beneficio-1")).thenReturn(true);

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-abc-123", "beneficio-1", BigDecimal.TEN);

        assertThatThrownBy(() -> service.validarYUsarBeneficio("comercio-1", request))
                .isInstanceOf(BeneficioYaCanjeadoException.class);
        verify(historialBeneficioRepository, never()).save(any(HistorialBeneficio.class));
    }

    // ---------- listarBeneficiosVigentes ----------

    @Test
    void listarBeneficiosVigentes_filtraInactivosYVencidos() {
        Beneficio vigente = beneficioVigente("beneficio-1", "comercio-1");
        Beneficio vencido = beneficioVigente("beneficio-2", "comercio-1");
        vencido.setFechaFinVigencia(LocalDate.now().minusDays(1));

        when(beneficioRepository.findByEstado(EstadoBeneficio.ACTIVO)).thenReturn(List.of(vigente, vencido));

        List<BeneficioResumenResponse> resultado = service.listarBeneficiosVigentes(null, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo("beneficio-1");
    }

    @Test
    void listarBeneficiosVigentes_filtraPorRubro() {
        Beneficio farmacia = beneficioVigente("beneficio-1", "comercio-1");
        Beneficio otroRubro = beneficioVigente("beneficio-2", "comercio-2");
        otroRubro.setComercioRubro("Gastronomía");

        when(beneficioRepository.findByEstado(EstadoBeneficio.ACTIVO)).thenReturn(List.of(farmacia, otroRubro));

        List<BeneficioResumenResponse> resultado = service.listarBeneficiosVigentes("Farmacia", null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).comercioRubro()).isEqualTo("Farmacia");
    }

    @Test
    void listarBeneficiosVigentes_filtraPorBusqueda() {
        Beneficio coincide = beneficioVigente("beneficio-1", "comercio-1");
        Beneficio noCoincide = beneficioVigente("beneficio-2", "comercio-1");
        noCoincide.setTitulo("2x1 en menú ejecutivo");
        noCoincide.setComercioNombre("Restaurante La Pampa");

        when(beneficioRepository.findByEstado(EstadoBeneficio.ACTIVO)).thenReturn(List.of(coincide, noCoincide));

        List<BeneficioResumenResponse> resultado = service.listarBeneficiosVigentes(null, "medicamentos");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo("beneficio-1");
    }

    // ---------- listarComerciosConBeneficios ----------

    @Test
    void listarComerciosConBeneficios_agrupaPorComercio() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        when(beneficioRepository.findByEstado(EstadoBeneficio.ACTIVO)).thenReturn(List.of(beneficio));
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio()));

        List<ComercioConBeneficiosResponse> resultado = service.listarComerciosConBeneficios(null, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombreComercial()).isEqualTo("Farmacia Del Sol");
        assertThat(resultado.get(0).beneficios()).hasSize(1);
    }

    // ---------- listarHistorialDeSocio ----------

    @Test
    void listarHistorialDeSocio_devuelveElHistorialDelSocio() {
        HistorialBeneficio historial = HistorialBeneficio.builder()
                .id("hist-1")
                .beneficioTitulo("15% en medicamentos")
                .tipo(TipoBeneficio.DESCUENTO_PORCENTAJE)
                .valor("15%")
                .comercioNombre("Farmacia Del Sol")
                .socioId("socio-1")
                .montoAhorro(new BigDecimal("450.00"))
                .estado(EstadoUsoBeneficio.USADO)
                .fechaUso(Instant.now())
                .build();
        when(historialBeneficioRepository.findBySocioIdOrderByFechaUsoDesc("socio-1")).thenReturn(List.of(historial));

        List<HistorialBeneficioResponse> resultado = service.listarHistorialDeSocio("socio-1");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).comercioNombre()).isEqualTo("Farmacia Del Sol");
        assertThat(resultado.get(0).montoAhorro()).isEqualByComparingTo("450.00");
    }
}
