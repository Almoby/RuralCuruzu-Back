package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoBeneficio;
import com.almoby.ruralcuruzu.enums.EstadoComercio;
import com.almoby.ruralcuruzu.enums.EstadoQr;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.EstadoUsoBeneficio;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.exception.BeneficioNoEncontradoException;
import com.almoby.ruralcuruzu.exception.BeneficioNoVigenteException;
import com.almoby.ruralcuruzu.exception.BeneficioYaCanjeadoException;
import com.almoby.ruralcuruzu.exception.CodigoQrExpiradoException;
import com.almoby.ruralcuruzu.exception.CodigoQrInvalidoException;
import com.almoby.ruralcuruzu.exception.ComercioNoEncontradoException;
import com.almoby.ruralcuruzu.exception.QrNoValidoException;
import com.almoby.ruralcuruzu.exception.TipoBeneficioInvalidoException;
import com.almoby.ruralcuruzu.repository.BeneficioRepository;
import com.almoby.ruralcuruzu.repository.ComercioRepository;
import com.almoby.ruralcuruzu.repository.HistorialBeneficioRepository;
import com.almoby.ruralcuruzu.repository.SocioRepository;
import com.almoby.ruralcuruzu.repository.TipoBeneficioCatalogoRepository;
import com.almoby.ruralcuruzu.security.jwt.QrTokenService;
import com.almoby.ruralcuruzu.service.EstadoQrService;

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
    @Mock
    private EstadoQrService estadoQrService;
    @Mock
    private QrTokenService qrTokenService;
    @Mock
    private TipoBeneficioCatalogoRepository tipoBeneficioCatalogoRepository;

    private BeneficioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BeneficioServiceImpl(beneficioRepository, historialBeneficioRepository, comercioRepository,
                socioRepository, estadoQrService, qrTokenService, tipoBeneficioCatalogoRepository);
        // Default: "tipo-1" resuelve a un tipo activo, salvo que un test lo pise (evita
        // repetir este stub en cada test de crear/actualizar que no lo pone a prueba).
        lenient().when(tipoBeneficioCatalogoRepository.findById("tipo-1")).thenReturn(Optional.of(tipoBeneficioActivo()));
    }

    private TipoBeneficioCatalogo tipoBeneficioActivo() {
        return TipoBeneficioCatalogo.builder()
                .id("tipo-1")
                .codigo("DESCUENTO_PORCENTAJE")
                .nombre("Descuento por porcentaje")
                .activo(true)
                .build();
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
                .tipoBeneficioId("tipo-1")
                .tipoBeneficioNombre("Descuento por porcentaje")
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
                .build();
    }

    // ---------- crearBeneficio ----------

    @Test
    void crearBeneficio_conComercioExistente_loCreaActivo() {
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio()));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearBeneficioRequest request = new CrearBeneficioRequest(
                "15% en medicamentos", "Descuento en venta libre", "tipo-1", "15%",
                null, LocalDate.of(2026, 12, 31));

        BeneficioCreadoResponse response = service.crearBeneficio("comercio-1", request);

        assertThat(response.mensaje()).isEqualTo("Beneficio creado con éxito");
        assertThat(response.beneficio().comercioNombre()).isEqualTo("Farmacia Del Sol");
        assertThat(response.beneficio().comercioId()).isEqualTo("comercio-1");
        assertThat(response.beneficio().estado()).isEqualTo(EstadoBeneficio.ACTIVO);
    }

    @Test
    void crearBeneficio_conFechaInicioFutura_naceInactivoEnLaBase() {
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio()));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearBeneficioRequest request = new CrearBeneficioRequest(
                "15% en medicamentos", "Descuento en venta libre", "tipo-1", "15%",
                LocalDate.now().plusDays(7), null);

        BeneficioCreadoResponse response = service.crearBeneficio("comercio-1", request);

        // El campo crudo (no solo el estadoEfectivo() de la API) queda INACTIVO desde ya.
        assertThat(response.beneficio().estado()).isEqualTo(EstadoBeneficio.INACTIVO);
        ArgumentCaptor<Beneficio> captor = ArgumentCaptor.forClass(Beneficio.class);
        verify(beneficioRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoBeneficio.INACTIVO);
        assertThat(captor.getValue().isPausadoManualmente()).isFalse();
    }

    @Test
    void crearBeneficio_comercioInexistente_lanzaExcepcion() {
        when(comercioRepository.findById("no-existe")).thenReturn(Optional.empty());

        CrearBeneficioRequest request = new CrearBeneficioRequest(
                "titulo", "desc", "tipo-1", "Gratis", null, null);

        assertThatThrownBy(() -> service.crearBeneficio("no-existe", request))
                .isInstanceOf(ComercioNoEncontradoException.class);
    }

    @Test
    void crearBeneficio_tipoBeneficioInexistente_lanzaExcepcion() {
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio()));
        when(tipoBeneficioCatalogoRepository.findById("no-existe")).thenReturn(Optional.empty());

        CrearBeneficioRequest request = new CrearBeneficioRequest(
                "titulo", "desc", "no-existe", "Gratis", null, null);

        assertThatThrownBy(() -> service.crearBeneficio("comercio-1", request))
                .isInstanceOf(TipoBeneficioInvalidoException.class);
        verify(beneficioRepository, never()).save(any(Beneficio.class));
    }

    @Test
    void crearBeneficio_tipoBeneficioInactivo_lanzaExcepcion() {
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio()));
        TipoBeneficioCatalogo inactivo = TipoBeneficioCatalogo.builder()
                .id("tipo-inactivo").codigo("VIEJO").nombre("Viejo").activo(false).build();
        when(tipoBeneficioCatalogoRepository.findById("tipo-inactivo")).thenReturn(Optional.of(inactivo));

        CrearBeneficioRequest request = new CrearBeneficioRequest(
                "titulo", "desc", "tipo-inactivo", "Gratis", null, null);

        assertThatThrownBy(() -> service.crearBeneficio("comercio-1", request))
                .isInstanceOf(TipoBeneficioInvalidoException.class);
        verify(beneficioRepository, never()).save(any(Beneficio.class));
    }

    // ---------- listar / obtener / actualizar / cambiar estado (ownership) ----------

    @Test
    void listarBeneficiosDelComercio_calculaUsosEsteMesPorBeneficioEnUnaSolaConsulta() {
        Beneficio conUsos = beneficioVigente("beneficio-1", "comercio-1");
        Beneficio sinUsos = beneficioVigente("beneficio-2", "comercio-1");
        when(beneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of(conUsos, sinUsos));
        when(historialBeneficioRepository.findByComercioIdAndFechaUsoAfter(eq("comercio-1"), any(Instant.class)))
                .thenReturn(List.of(
                        HistorialBeneficio.builder().beneficioId("beneficio-1").build(),
                        HistorialBeneficio.builder().beneficioId("beneficio-1").build()));

        List<BeneficioResponse> respuesta = service.listarBeneficiosDelComercio("comercio-1");

        assertThat(respuesta).hasSize(2);
        assertThat(respuesta.stream().filter(b -> b.id().equals("beneficio-1")).findFirst().orElseThrow().usosEsteMes())
                .isEqualTo(2L);
        assertThat(respuesta.stream().filter(b -> b.id().equals("beneficio-2")).findFirst().orElseThrow().usosEsteMes())
                .isEqualTo(0L);
    }

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
                "20% en medicamentos", "nueva desc", "tipo-1", "20%", null, null);

        BeneficioResponse response = service.actualizarBeneficio("comercio-1", "beneficio-1", request);

        assertThat(response.titulo()).isEqualTo("20% en medicamentos");
        assertThat(response.valor()).isEqualTo("20%");
    }

    @Test
    void actualizarBeneficio_vencidoYReactivadoConNuevaFecha_seReactivaSolo() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        beneficio.setEstado(EstadoBeneficio.INACTIVO); // lo dejó así el job diario al vencer
        beneficio.setFechaFinVigencia(LocalDate.now().minusDays(1));
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarBeneficioRequest request = new ActualizarBeneficioRequest(
                "15% en medicamentos", "desc", "tipo-1", "15%",
                null, LocalDate.now().plusDays(30));

        BeneficioResponse response = service.actualizarBeneficio("comercio-1", "beneficio-1", request);

        assertThat(response.estado()).isEqualTo(EstadoBeneficio.ACTIVO);
    }

    @Test
    void actualizarBeneficio_pausadoAMano_noSeReactivaSoloAunqueLaFechaSigaVigente() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        beneficio.setEstado(EstadoBeneficio.INACTIVO); // el comercio lo pausó, no venció
        beneficio.setPausadoManualmente(true);
        beneficio.setFechaFinVigencia(LocalDate.now().plusDays(30));
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarBeneficioRequest request = new ActualizarBeneficioRequest(
                "15% en medicamentos", "desc", "tipo-1", "15%",
                null, LocalDate.now().plusDays(60));

        BeneficioResponse response = service.actualizarBeneficio("comercio-1", "beneficio-1", request);

        assertThat(response.estado()).isEqualTo(EstadoBeneficio.INACTIVO);
    }

    @Test
    void actualizarBeneficio_pausadoAMuchoDespuesDeVencer_tampocoSeReactivaSolo() {
        // Caso límite: el comercio pausó a propósito un beneficio que YA estaba vencido
        // (pausadoManualmente=true, no solo estado=INACTIVO). Sin la marca de pausa manual,
        // esto se confundiría con "lo venció el job" y se reactivaría solo al extender la fecha.
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        beneficio.setEstado(EstadoBeneficio.INACTIVO);
        beneficio.setPausadoManualmente(true);
        beneficio.setFechaFinVigencia(LocalDate.now().minusDays(1));
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarBeneficioRequest request = new ActualizarBeneficioRequest(
                "15% en medicamentos", "desc", "tipo-1", "15%",
                null, LocalDate.now().plusDays(30));

        BeneficioResponse response = service.actualizarBeneficio("comercio-1", "beneficio-1", request);

        assertThat(response.estado()).isEqualTo(EstadoBeneficio.INACTIVO);
    }

    @Test
    void actualizarBeneficio_noHabiaEmpezadoYLeAdelantanLaFecha_seActivaSolo() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        beneficio.setEstado(EstadoBeneficio.INACTIVO); // todavía no había llegado fechaInicioVigencia
        beneficio.setFechaInicioVigencia(LocalDate.now().plusDays(7));
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarBeneficioRequest request = new ActualizarBeneficioRequest(
                "15% en medicamentos", "desc", "tipo-1", "15%",
                LocalDate.now(), null); // adelantan el inicio a hoy

        BeneficioResponse response = service.actualizarBeneficio("comercio-1", "beneficio-1", request);

        assertThat(response.estado()).isEqualTo(EstadoBeneficio.ACTIVO);
    }

    @Test
    void actualizarBeneficio_activoYLeAtrasanLaFechaDeInicio_seDesactivaSolo() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1"); // estado=ACTIVO, sin fechas
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarBeneficioRequest request = new ActualizarBeneficioRequest(
                "15% en medicamentos", "desc", "tipo-1", "15%",
                LocalDate.now().plusDays(7), null); // atrasan el inicio a la semana que viene

        BeneficioResponse response = service.actualizarBeneficio("comercio-1", "beneficio-1", request);

        assertThat(response.estado()).isEqualTo(EstadoBeneficio.INACTIVO);
        assertThat(beneficio.isPausadoManualmente()).isFalse(); // quedó inactivo por fecha, no por pausa manual
    }

    @Test
    void actualizarBeneficio_pausadoAMano_noSeDesactivaSoloAunqueLeAtrasenElInicio() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        beneficio.setEstado(EstadoBeneficio.INACTIVO);
        beneficio.setPausadoManualmente(true); // el comercio lo pausó a mano, no por fecha
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarBeneficioRequest request = new ActualizarBeneficioRequest(
                "15% en medicamentos", "desc", "tipo-1", "15%",
                null, null); // fechas que lo dejarían vigente hoy si no estuviera pausado

        BeneficioResponse response = service.actualizarBeneficio("comercio-1", "beneficio-1", request);

        assertThat(response.estado()).isEqualTo(EstadoBeneficio.INACTIVO);
    }

    @Test
    void obtenerBeneficioDelComercio_conFechaInicioFutura_apareceInactivoHastaEseDia() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        beneficio.setFechaInicioVigencia(LocalDate.now().plusDays(7)); // todavía no arrancó
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));

        BeneficioResponse antesDeEmpezar = service.obtenerBeneficioDelComercio("comercio-1", "beneficio-1");
        assertThat(antesDeEmpezar.estado()).isEqualTo(EstadoBeneficio.INACTIVO);

        // Sin tocar nada más (ni el job, ni un PATCH /estado): el mismo día que arranca la
        // vigencia, ya se ve ACTIVO, porque el cálculo es en vivo (Beneficio.estaVigenteHoy()).
        beneficio.setFechaInicioVigencia(LocalDate.now());
        BeneficioResponse elDiaQueArranca = service.obtenerBeneficioDelComercio("comercio-1", "beneficio-1");
        assertThat(elDiaQueArranca.estado()).isEqualTo(EstadoBeneficio.ACTIVO);
    }

    @Test
    void cambiarEstadoBeneficio_propio_loActualiza() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        BeneficioResponse response = service.cambiarEstadoBeneficio(
                "comercio-1", "beneficio-1", new CambiarEstadoBeneficioRequest(EstadoBeneficio.INACTIVO));

        assertThat(response.estado()).isEqualTo(EstadoBeneficio.INACTIVO);
        assertThat(beneficio.isPausadoManualmente()).isTrue();
    }

    @Test
    void cambiarEstadoBeneficio_reactivar_limpiaLaMarcaDePausaManual() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        beneficio.setEstado(EstadoBeneficio.INACTIVO);
        beneficio.setPausadoManualmente(true);
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cambiarEstadoBeneficio("comercio-1", "beneficio-1", new CambiarEstadoBeneficioRequest(EstadoBeneficio.ACTIVO));

        assertThat(beneficio.isPausadoManualmente()).isFalse();
    }

    // ---------- validarYUsarBeneficio ----------

    @Test
    void validarYUsarBeneficio_qrYBeneficioValidos_creaHistorialYDevuelveConfirmacion() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        when(qrTokenService.extraerSocioId("qr-abc-123")).thenReturn("socio-1");
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio()));
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));
        when(historialBeneficioRepository.existsBySocioIdAndBeneficioId("socio-1", "beneficio-1")).thenReturn(false);
        when(historialBeneficioRepository.save(any(HistorialBeneficio.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-abc-123", "beneficio-1", new BigDecimal("450.00"));

        ValidarBeneficioResponse response = service.validarYUsarBeneficio("comercio-1", "usuario-comercio-1", request);

        assertThat(response.mensaje()).isEqualTo("Beneficio aplicado con éxito");
        assertThat(response.socioNombre()).isEqualTo("Pérez, Juan");
        assertThat(response.socioNumeroSocio()).isEqualTo("SOC-000001");
        assertThat(response.socioCategoria()).isEqualTo(CategoriaSocio.ACTIVO);
        assertThat(response.beneficioTitulo()).isEqualTo("15% en medicamentos");
        assertThat(response.beneficioTipoNombre()).isEqualTo("Descuento por porcentaje");
        assertThat(response.beneficioValor()).isEqualTo("15%");
        assertThat(response.montoAhorro()).isEqualByComparingTo("450.00");

        ArgumentCaptor<HistorialBeneficio> captor = ArgumentCaptor.forClass(HistorialBeneficio.class);
        verify(historialBeneficioRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoUsoBeneficio.USADO);
        assertThat(captor.getValue().getSocioId()).isEqualTo("socio-1");
        assertThat(captor.getValue().getUsuarioComercioId()).isEqualTo("usuario-comercio-1");
    }

    @Test
    void validarYUsarBeneficio_qrNoActivo_lanzaExcepcionYNoCreaHistorial() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        when(qrTokenService.extraerSocioId("qr-abc-123")).thenReturn("socio-1");
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio()));
        doThrow(new QrNoValidoException(EstadoQr.INACTIVO_POR_DEUDA,
                "Tenés cuotas vencidas. Regularizá tu situación para volver a usar el QR."))
                .when(estadoQrService).validarQrActivo(any(Socio.class));

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-abc-123", "beneficio-1", new BigDecimal("450.00"));

        assertThatThrownBy(() -> service.validarYUsarBeneficio("comercio-1", "usuario-comercio-1", request))
                .isInstanceOf(QrNoValidoException.class)
                .hasMessage("Tenés cuotas vencidas. Regularizá tu situación para volver a usar el QR.");

        verify(historialBeneficioRepository, never()).save(any(HistorialBeneficio.class));
        verify(beneficioRepository, never()).findById(anyString());
    }

    @Test
    void validarYUsarBeneficio_tokenQrInvalido_lanzaExcepcion() {
        when(qrTokenService.extraerSocioId("qr-invalido")).thenThrow(new CodigoQrInvalidoException());

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-invalido", "beneficio-1", BigDecimal.TEN);

        assertThatThrownBy(() -> service.validarYUsarBeneficio("comercio-1", "usuario-comercio-1", request))
                .isInstanceOf(CodigoQrInvalidoException.class);
        verify(socioRepository, never()).findById(anyString());
    }

    @Test
    void validarYUsarBeneficio_tokenQrExpirado_lanzaExcepcion() {
        when(qrTokenService.extraerSocioId("qr-vencido")).thenThrow(new CodigoQrExpiradoException());

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-vencido", "beneficio-1", BigDecimal.TEN);

        assertThatThrownBy(() -> service.validarYUsarBeneficio("comercio-1", "usuario-comercio-1", request))
                .isInstanceOf(CodigoQrExpiradoException.class);
    }

    @Test
    void validarYUsarBeneficio_tokenValidoPeroSocioYaNoExiste_lanzaExcepcion() {
        when(qrTokenService.extraerSocioId("qr-abc-123")).thenReturn("socio-borrado");
        when(socioRepository.findById("socio-borrado")).thenReturn(Optional.empty());

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-abc-123", "beneficio-1", BigDecimal.TEN);

        assertThatThrownBy(() -> service.validarYUsarBeneficio("comercio-1", "usuario-comercio-1", request))
                .isInstanceOf(CodigoQrInvalidoException.class);
    }

    @Test
    void validarYUsarBeneficio_beneficioDeOtroComercio_lanzaExcepcion() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-ajeno");
        when(qrTokenService.extraerSocioId("qr-abc-123")).thenReturn("socio-1");
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio()));
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-abc-123", "beneficio-1", BigDecimal.TEN);

        assertThatThrownBy(() -> service.validarYUsarBeneficio("comercio-1", "usuario-comercio-1", request))
                .isInstanceOf(BeneficioNoEncontradoException.class);
    }

    @Test
    void validarYUsarBeneficio_beneficioPausado_lanzaExcepcion() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        beneficio.setEstado(EstadoBeneficio.INACTIVO);
        when(qrTokenService.extraerSocioId("qr-abc-123")).thenReturn("socio-1");
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio()));
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-abc-123", "beneficio-1", BigDecimal.TEN);

        assertThatThrownBy(() -> service.validarYUsarBeneficio("comercio-1", "usuario-comercio-1", request))
                .isInstanceOf(BeneficioNoVigenteException.class);
    }

    @Test
    void validarYUsarBeneficio_beneficioVencido_lanzaExcepcion() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        beneficio.setFechaFinVigencia(LocalDate.now().minusDays(1));
        when(qrTokenService.extraerSocioId("qr-abc-123")).thenReturn("socio-1");
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio()));
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-abc-123", "beneficio-1", BigDecimal.TEN);

        assertThatThrownBy(() -> service.validarYUsarBeneficio("comercio-1", "usuario-comercio-1", request))
                .isInstanceOf(BeneficioNoVigenteException.class);
    }

    @Test
    void validarYUsarBeneficio_socioYaLoHabiaCanjeadoAntes_lanzaExcepcionYNoDuplicaElRegistro() {
        Beneficio beneficio = beneficioVigente("beneficio-1", "comercio-1");
        when(qrTokenService.extraerSocioId("qr-abc-123")).thenReturn("socio-1");
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio()));
        when(beneficioRepository.findById("beneficio-1")).thenReturn(Optional.of(beneficio));
        when(historialBeneficioRepository.existsBySocioIdAndBeneficioId("socio-1", "beneficio-1")).thenReturn(true);

        ValidarBeneficioRequest request = new ValidarBeneficioRequest("qr-abc-123", "beneficio-1", BigDecimal.TEN);

        assertThatThrownBy(() -> service.validarYUsarBeneficio("comercio-1", "usuario-comercio-1", request))
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
                .tipoBeneficioNombre("Descuento por porcentaje")
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

    // ---------- marcarBeneficiosVencidos ----------

    @Test
    void marcarBeneficiosVencidos_pasaAInactivoLoQueYaVencio() {
        Beneficio vencido = beneficioVigente("beneficio-1", "comercio-1");
        vencido.setFechaFinVigencia(LocalDate.now().minusDays(1));
        when(beneficioRepository.findByEstadoAndFechaFinVigenciaBefore(eq(EstadoBeneficio.ACTIVO), any(LocalDate.class)))
                .thenReturn(List.of(vencido));

        service.marcarBeneficiosVencidos();

        assertThat(vencido.getEstado()).isEqualTo(EstadoBeneficio.INACTIVO);
        verify(beneficioRepository).save(vencido);
    }

    @Test
    void marcarBeneficiosVencidos_sinNadaVencido_noGuardaNada() {
        when(beneficioRepository.findByEstadoAndFechaFinVigenciaBefore(eq(EstadoBeneficio.ACTIVO), any(LocalDate.class)))
                .thenReturn(List.of());

        service.marcarBeneficiosVencidos();

        verify(beneficioRepository, never()).save(any(Beneficio.class));
    }

    // ---------- activarBeneficiosQueEmpiezanHoy ----------

    @Test
    void activarBeneficiosQueEmpiezanHoy_activaLosQueYaEmpezaronSuVigencia() {
        Beneficio empezoHoy = beneficioVigente("beneficio-1", "comercio-1");
        empezoHoy.setEstado(EstadoBeneficio.INACTIVO);
        empezoHoy.setFechaInicioVigencia(LocalDate.now());
        when(beneficioRepository.findByEstadoAndPausadoManualmenteFalseAndFechaInicioVigenciaLessThanEqual(
                eq(EstadoBeneficio.INACTIVO), any(LocalDate.class)))
                .thenReturn(List.of(empezoHoy));

        service.activarBeneficiosQueEmpiezanHoy();

        assertThat(empezoHoy.getEstado()).isEqualTo(EstadoBeneficio.ACTIVO);
        verify(beneficioRepository).save(empezoHoy);
    }

    @Test
    void activarBeneficiosQueEmpiezanHoy_siYaVencioTambien_noLoActiva() {
        // Caso límite: fechaInicioVigencia <= hoy pero fechaFinVigencia también ya pasó
        // (toda la ventana de vigencia quedó en el pasado). No tiene sentido activarlo
        // para que el otro job lo vuelva a apagar recién a la medianoche siguiente.
        Beneficio ventanaVencida = beneficioVigente("beneficio-1", "comercio-1");
        ventanaVencida.setEstado(EstadoBeneficio.INACTIVO);
        ventanaVencida.setFechaInicioVigencia(LocalDate.now().minusDays(10));
        ventanaVencida.setFechaFinVigencia(LocalDate.now().minusDays(1));
        when(beneficioRepository.findByEstadoAndPausadoManualmenteFalseAndFechaInicioVigenciaLessThanEqual(
                eq(EstadoBeneficio.INACTIVO), any(LocalDate.class)))
                .thenReturn(List.of(ventanaVencida));

        service.activarBeneficiosQueEmpiezanHoy();

        verify(beneficioRepository, never()).save(any(Beneficio.class));
    }

    @Test
    void activarBeneficiosQueEmpiezanHoy_sinNadaParaActivar_noGuardaNada() {
        when(beneficioRepository.findByEstadoAndPausadoManualmenteFalseAndFechaInicioVigenciaLessThanEqual(
                eq(EstadoBeneficio.INACTIVO), any(LocalDate.class)))
                .thenReturn(List.of());

        service.activarBeneficiosQueEmpiezanHoy();

        verify(beneficioRepository, never()).save(any(Beneficio.class));
    }
}
