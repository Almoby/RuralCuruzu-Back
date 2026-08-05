package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import com.almoby.ruralcuruzu.domain.Cuota;
import com.almoby.ruralcuruzu.domain.DatosPersonaFisica;
import com.almoby.ruralcuruzu.domain.EjecucionGeneracionCuotas;
import com.almoby.ruralcuruzu.domain.Pago;
import com.almoby.ruralcuruzu.domain.ReglaCuota;
import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.dto.request.AnularCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.InformarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RegistrarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RevisarPagoInformadoRequest;
import com.almoby.ruralcuruzu.dto.response.CuotaResponse;
import com.almoby.ruralcuruzu.dto.response.CuotaResumenResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoCuentaSocioResponse;
import com.almoby.ruralcuruzu.dto.response.GeneracionCuotasResponse;
import com.almoby.ruralcuruzu.dto.response.InformarPagoResponse;
import com.almoby.ruralcuruzu.dto.response.LinkDePagoResponse;
import com.almoby.ruralcuruzu.dto.response.RegistrarPagoResponse;
import com.almoby.ruralcuruzu.dto.response.RevisarPagoInformadoResponse;
import com.almoby.ruralcuruzu.dto.response.ResumenCuotasResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoCuota;
import com.almoby.ruralcuruzu.enums.EstadoPago;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.MedioPago;
import com.almoby.ruralcuruzu.enums.OrigenEjecucionCuotas;
import com.almoby.ruralcuruzu.enums.Rol;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.exception.ArchivoInvalidoException;
import com.almoby.ruralcuruzu.exception.CuotaEstadoInvalidoException;
import com.almoby.ruralcuruzu.exception.CuotaNoEncontradaException;
import com.almoby.ruralcuruzu.exception.PagoNoEncontradoException;
import com.almoby.ruralcuruzu.exception.SocioNoEncontradoException;
import com.almoby.ruralcuruzu.repository.CuotaRepository;
import com.almoby.ruralcuruzu.repository.EjecucionGeneracionCuotasRepository;
import com.almoby.ruralcuruzu.repository.PagoRepository;
import com.almoby.ruralcuruzu.repository.ReglaCuotaRepository;
import com.almoby.ruralcuruzu.repository.SocioRepository;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.service.AlmacenamientoComprobantesService;
import com.almoby.ruralcuruzu.service.ComprobanteService;
import com.almoby.ruralcuruzu.service.EmailService;
import com.almoby.ruralcuruzu.service.EstadoPagoMercadoPago;
import com.almoby.ruralcuruzu.service.MercadoPagoService;
import com.almoby.ruralcuruzu.service.PreferenciaMercadoPago;

@ExtendWith(MockitoExtension.class)
class CuotaServiceImplTest {

    @Mock
    private CuotaRepository cuotaRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private EjecucionGeneracionCuotasRepository ejecucionRepository;
    @Mock
    private SocioRepository socioRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private ReglaCuotaRepository reglaCuotaRepository;
    @Mock
    private AlmacenamientoComprobantesService almacenamientoComprobantesService;
    @Mock
    private MercadoPagoService mercadoPagoService;
    @Mock
    private ComprobanteService comprobanteService;
    @Mock
    private UsuarioRepository usuarioRepository;

    private CuotaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CuotaServiceImpl(cuotaRepository, pagoRepository, ejecucionRepository, socioRepository,
                emailService, reglaCuotaRepository, almacenamientoComprobantesService, mercadoPagoService,
                comprobanteService, usuarioRepository);

        // Default: ningún pago vigente salvo que un test lo indique explícitamente
        // (evita repetir este stub en cada test que no necesita un Pago).
        lenient().when(pagoRepository.findByCuotaIdIn(anyList())).thenReturn(List.of());
        lenient().when(pagoRepository.findByCuotaId(anyString())).thenReturn(List.of());
    }

    private ReglaCuota reglaCuota(CategoriaSocio categoria, String nombre, String importe, int diaVencimiento) {
        return ReglaCuota.builder()
                .id("regla-" + categoria)
                .categoriaAplicable(categoria)
                .nombre(nombre)
                .importe(new BigDecimal(importe))
                .diaVencimiento(diaVencimiento)
                .build();
    }

    private Socio socioActivo(String id, String numeroSocio, CategoriaSocio categoria) {
        return socioActivo(id, numeroSocio, categoria, "juan@example.com");
    }

    /** Igual que {@link #socioActivo(String, String, CategoriaSocio)}, pero con email explícito (puede ser null). */
    private Socio socioActivo(String id, String numeroSocio, CategoriaSocio categoria, String email) {
        DatosPersonaFisica datos = new DatosPersonaFisica(
                "Lopez, Juan", "12345678", null, null, null, null, null, email, null, null);
        return Socio.builder()
                .id(id)
                .numeroSocio(numeroSocio)
                .categoria(categoria)
                .tipoPersona(TipoPersona.FISICA)
                .datosPersonaFisica(datos)
                .estado(EstadoSocio.ACTIVO)
                .build();
    }

    private Cuota cuotaPendiente(String id, String socioId) {
        return cuotaPendiente(id, socioId, "2026-07");
    }

    private Cuota cuotaPendiente(String id, String socioId, String periodo) {
        return Cuota.builder()
                .id(id)
                .socioId(socioId)
                .socioNumeroSocio("SOC-000001")
                .socioNombre("Lopez, Juan")
                .tipoCuotaNombre("Cuota de socio activo")
                .categoria(CategoriaSocio.ACTIVO)
                .periodo(periodo)
                .importe(new BigDecimal("15000.00"))
                .fechaVencimiento(LocalDate.of(2026, 7, 10))
                .estado(EstadoCuota.PENDIENTE)
                .build();
    }

    private Pago pagoAprobado(String id, String cuotaId, String importe, MedioPago medioPago) {
        return Pago.builder()
                .id(id)
                .cuotaId(cuotaId)
                .importe(new BigDecimal(importe))
                .medioPago(medioPago)
                .estado(EstadoPago.APROBADO)
                .build();
    }

    private MultipartFile comprobantePdf() {
        return new MockMultipartFile("comprobante", "comprobante.pdf", "application/pdf", "contenido".getBytes());
    }

    // ---------- generarCuotas ----------

    @Test
    void generarCuotas_conSocioActivo_generaCuotaPendienteConLaRegladeSuCategoria() {
        Socio socio = socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO);
        when(socioRepository.findByEstado(EstadoSocio.ACTIVO)).thenReturn(List.of(socio));
        when(cuotaRepository.findByPeriodo(anyString())).thenReturn(List.of());
        when(reglaCuotaRepository.findAll())
                .thenReturn(List.of(reglaCuota(CategoriaSocio.ACTIVO, "Cuota de socio activo", "15000.00", 10)));
        when(ejecucionRepository.save(any(EjecucionGeneracionCuotas.class))).thenAnswer(inv -> inv.getArgument(0));

        GeneracionCuotasResponse response = service.generarCuotas("2026-07", null, null);

        assertThat(response.cantidadCuotasGeneradas()).isEqualTo(1);
        assertThat(response.cantidadSociosOmitidos()).isEqualTo(0);
        assertThat(response.origen()).isEqualTo(OrigenEjecucionCuotas.AUTOMATICA);

        ArgumentCaptor<Cuota> cuotaCaptor = ArgumentCaptor.forClass(Cuota.class);
        verify(cuotaRepository).save(cuotaCaptor.capture());
        assertThat(cuotaCaptor.getValue().getImporte()).isEqualByComparingTo("15000.00");
        assertThat(cuotaCaptor.getValue().getTipoCuotaNombre()).isEqualTo("Cuota de socio activo");
        verify(emailService).enviarCorreoCuotaGenerada(eq("juan@example.com"), anyString(), eq("2026-07"), any(), any());
        // D-4/D-5: un único findAll()/findByPeriodo() por corrida, nunca por-socio.
        verify(reglaCuotaRepository, times(1)).findAll();
        verify(reglaCuotaRepository, never()).findByCategoriaAplicable(any());
        verify(cuotaRepository, times(1)).findByPeriodo("2026-07");
        verify(cuotaRepository, never()).existsBySocioIdAndPeriodo(anyString(), anyString());
    }

    @Test
    void generarCuotas_conAdminId_quedaComoOrigenManual() {
        when(socioRepository.findByEstado(EstadoSocio.ACTIVO)).thenReturn(List.of());
        when(ejecucionRepository.save(any(EjecucionGeneracionCuotas.class))).thenAnswer(inv -> inv.getArgument(0));

        GeneracionCuotasResponse response = service.generarCuotas("2026-07", "admin-1", "Admin Uno");

        assertThat(response.origen()).isEqualTo(OrigenEjecucionCuotas.MANUAL);
    }

    @Test
    void generarCuotas_conCategoriaSinReglaCargada_loOmiteYNoRompeLaCorrida() {
        Socio socio = socioActivo("socio-1", "SOC-000001", CategoriaSocio.ADHERENTE);
        when(socioRepository.findByEstado(EstadoSocio.ACTIVO)).thenReturn(List.of(socio));
        when(cuotaRepository.findByPeriodo(anyString())).thenReturn(List.of());
        when(reglaCuotaRepository.findAll()).thenReturn(List.of());
        when(ejecucionRepository.save(any(EjecucionGeneracionCuotas.class))).thenAnswer(inv -> inv.getArgument(0));

        GeneracionCuotasResponse response = service.generarCuotas("2026-07", null, null);

        assertThat(response.cantidadCuotasGeneradas()).isEqualTo(0);
        assertThat(response.cantidadSociosOmitidos()).isEqualTo(1);
        verify(cuotaRepository, never()).save(any());
    }

    @Test
    void generarCuotas_conSocioQueYaTieneCuotaEnElPeriodo_noLaDuplica() {
        Socio socio = socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO);
        when(socioRepository.findByEstado(EstadoSocio.ACTIVO)).thenReturn(List.of(socio));
        when(cuotaRepository.findByPeriodo("2026-07")).thenReturn(List.of(cuotaPendiente("cuota-existente", "socio-1")));
        when(ejecucionRepository.save(any(EjecucionGeneracionCuotas.class))).thenAnswer(inv -> inv.getArgument(0));

        GeneracionCuotasResponse response = service.generarCuotas("2026-07", null, null);

        assertThat(response.cantidadCuotasGeneradas()).isEqualTo(0);
        verify(cuotaRepository, never()).save(any());
        verify(cuotaRepository, never()).existsBySocioIdAndPeriodo(anyString(), anyString());
    }

    // ---------- listarEjecuciones ----------

    @Test
    void listarEjecuciones_devuelveElHistorialMasRecientePrimero() {
        EjecucionGeneracionCuotas ejecucion = EjecucionGeneracionCuotas.builder()
                .id("ejec-1")
                .fechaEjecucion(Instant.parse("2026-08-01T09:00:00Z"))
                .origen(OrigenEjecucionCuotas.AUTOMATICA)
                .periodo("2026-08")
                .cantidadSociosActivos(2)
                .cantidadCuotasGeneradas(2)
                .cantidadSociosOmitidos(0)
                .build();
        when(ejecucionRepository.findAllByOrderByFechaEjecucionDesc()).thenReturn(List.of(ejecucion));

        List<GeneracionCuotasResponse> resultado = service.listarEjecuciones();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).periodo()).isEqualTo("2026-08");
        assertThat(resultado.get(0).origen()).isEqualTo(OrigenEjecucionCuotas.AUTOMATICA);
        assertThat(resultado.get(0).cantidadCuotasGeneradas()).isEqualTo(2);
    }

    // ---------- listarCuotas / obtenerCuotaPorId ----------

    @Test
    void obtenerCuotaPorId_inexistente_lanzaExcepcion() {
        when(cuotaRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerCuotaPorId("no-existe"))
                .isInstanceOf(CuotaNoEncontradaException.class);
    }

    @Test
    void obtenerCuotaPorId_conDosPagosEnRevisionParaLaMismaCuota_noRompeYMuestraElMasReciente() {
        // Regresión: con dos links de pago generados para la misma cuota (posible
        // desde que generar un link dejó de bloquearla), esto antes tiraba
        // IncorrectResultSizeDataAccessException con solo abrir la pantalla de la
        // cuota, mucho antes de llegar a informar/revisar nada.
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        Pago linkViejo = Pago.builder().id("pago-viejo").cuotaId("cuota-1")
                .estado(EstadoPago.EN_REVISION).fechaCreacion(Instant.now().minusSeconds(3600)).build();
        Pago linkNuevo = Pago.builder().id("pago-nuevo").cuotaId("cuota-1")
                .estado(EstadoPago.EN_REVISION).fechaCreacion(Instant.now()).build();
        when(pagoRepository.findByCuotaId("cuota-1")).thenReturn(List.of(linkViejo, linkNuevo));

        CuotaResponse response = service.obtenerCuotaPorId("cuota-1");

        assertThat(response.pagoVigente()).isNotNull();
        assertThat(response.pagoVigente().id()).isEqualTo("pago-nuevo");
    }

    @Test
    void listarCuotas_filtraPorEstadoSocioYPeriodo() {
        Cuota coincide = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findBySocioId("socio-1")).thenReturn(List.of(coincide));

        List<?> resultado = service.listarCuotas(EstadoCuota.PENDIENTE, "socio-1", "2026-07");

        assertThat(resultado).hasSize(1);
    }

    @Test
    void listarCuotasDeSocio_incluyeElPagoVigenteDeLaCuotaPagada() {
        Cuota pagada = cuotaPendiente("cuota-1", "socio-1");
        pagada.setEstado(EstadoCuota.PAGADA);
        Pago pago = pagoAprobado("pago-1", "cuota-1", "15000.00", MedioPago.TRANSFERENCIA);
        when(cuotaRepository.findBySocioId("socio-1")).thenReturn(List.of(pagada));
        when(pagoRepository.findByCuotaIdIn(List.of("cuota-1"))).thenReturn(List.of(pago));

        List<CuotaResumenResponse> resultado = service.listarCuotasDeSocio("socio-1");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).pagoVigente()).isNotNull();
        assertThat(resultado.get(0).pagoVigente().medioPago()).isEqualTo(MedioPago.TRANSFERENCIA);
    }

    @Test
    void listarCuotasDeSocio_conCuotaSinPagoInformado_devuelvePagoVigenteNulo() {
        Cuota pendiente = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findBySocioId("socio-1")).thenReturn(List.of(pendiente));

        List<CuotaResumenResponse> resultado = service.listarCuotasDeSocio("socio-1");

        assertThat(resultado.get(0).pagoVigente()).isNull();
    }

    // ---------- listarPagosDeSocio ----------

    @Test
    void listarPagosDeSocio_devuelveElHistorialCompletoMasRecientePrimero() {
        Pago pago = pagoAprobado("pago-1", "cuota-1", "15000.00", MedioPago.EFECTIVO);
        when(pagoRepository.findBySocioIdOrderByFechaCreacionDesc("socio-1")).thenReturn(List.of(pago));

        List<?> resultado = service.listarPagosDeSocio("socio-1");

        assertThat(resultado).hasSize(1);
    }

    // ---------- obtenerPagoPorId ----------

    @Test
    void obtenerPagoPorId_existente_devuelveElDetalleSinFiltrarPorSocio() {
        Pago pago = pagoAprobado("pago-1", "cuota-1", "15000.00", MedioPago.EFECTIVO);
        pago.setSocioId("socio-1");
        when(pagoRepository.findById("pago-1")).thenReturn(Optional.of(pago));

        PagoResponse resultado = service.obtenerPagoPorId("pago-1");

        assertThat(resultado.id()).isEqualTo("pago-1");
        assertThat(resultado.socioId()).isEqualTo("socio-1");
        assertThat(resultado.medioPago()).isEqualTo(MedioPago.EFECTIVO);
    }

    @Test
    void obtenerPagoPorId_inexistente_lanzaExcepcion() {
        when(pagoRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPagoPorId("no-existe"))
                .isInstanceOf(PagoNoEncontradoException.class)
                .hasMessageContaining("no-existe");
    }

    // ---------- registrarPago ----------

    @Test
    void registrarPago_conCuotaPendiente_marcaPagadaYMandaCorreo() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        Socio socio = socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO);
        when(cuotaRepository.findBySocioIdAndPeriodo("socio-1", "2026-07")).thenReturn(Optional.of(cuota));
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio));
        // Sin ninguna otra cuota en deuda para este socio (documento 29.1 "cuenta al día").
        when(cuotaRepository.findBySocioId("socio-1")).thenReturn(List.of(cuota));

        RegistrarPagoCuotaRequest request = new RegistrarPagoCuotaRequest(
                "socio-1", List.of("2026-07"), LocalDate.of(2026, 7, 5),
                MedioPago.TRANSFERENCIA, "COMP-1", "ok");

        RegistrarPagoResponse response = service.registrarPago(request, "admin-1", "Admin Uno");

        assertThat(response.mensaje()).isEqualTo("Pago registrado con éxito");
        assertThat(response.montoTotal()).isEqualByComparingTo("15000.00");
        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.PAGADA);

        ArgumentCaptor<Pago> pagoCaptor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(pagoCaptor.capture());
        assertThat(pagoCaptor.getValue().getImporte()).isEqualByComparingTo("15000.00");
        assertThat(pagoCaptor.getValue().getRegistradoPorAdminNombre()).isEqualTo("Admin Uno");
        assertThat(pagoCaptor.getValue().getEstado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(pagoCaptor.getValue().isInformadoPorSocio()).isFalse();
        verify(cuotaRepository).save(cuota);
        verify(emailService).enviarCorreoPagoRegistrado(eq("juan@example.com"), anyString(), eq("2026-07"), any());
        verify(emailService).enviarCorreoCuentaAlDia(eq("juan@example.com"), anyString());
    }

    @Test
    void registrarPago_conOtraCuotaPendienteDelSocio_noMandaCorreoDeCuentaAlDia() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        Cuota otraPendiente = cuotaPendiente("cuota-2", "socio-1", "2026-08");
        Socio socio = socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO);
        when(cuotaRepository.findBySocioIdAndPeriodo("socio-1", "2026-07")).thenReturn(Optional.of(cuota));
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio));
        when(cuotaRepository.findBySocioId("socio-1")).thenReturn(List.of(cuota, otraPendiente));

        RegistrarPagoCuotaRequest request = new RegistrarPagoCuotaRequest(
                "socio-1", List.of("2026-07"), LocalDate.of(2026, 7, 5),
                MedioPago.TRANSFERENCIA, "COMP-1", "ok");

        service.registrarPago(request, "admin-1", "Admin Uno");

        verify(emailService).enviarCorreoPagoRegistrado(eq("juan@example.com"), anyString(), eq("2026-07"), any());
        verify(emailService, never()).enviarCorreoCuentaAlDia(anyString(), anyString());
    }

    @Test
    void registrarPago_conCuotaYaPagada_lanzaExcepcion() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.PAGADA);
        when(cuotaRepository.findBySocioIdAndPeriodo("socio-1", "2026-07")).thenReturn(Optional.of(cuota));

        RegistrarPagoCuotaRequest request = new RegistrarPagoCuotaRequest(
                "socio-1", List.of("2026-07"), LocalDate.of(2026, 7, 5),
                MedioPago.EFECTIVO, null, null);

        assertThatThrownBy(() -> service.registrarPago(request, "admin-1", "Admin Uno"))
                .isInstanceOf(CuotaEstadoInvalidoException.class);
    }

    @Test
    void registrarPago_conVariosPeriodos_marcaTodasPagadas() {
        Cuota agosto = cuotaPendiente("cuota-1", "socio-1", "2026-08");
        Cuota septiembre = cuotaPendiente("cuota-2", "socio-1", "2026-09");
        Socio socio = socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO);
        when(cuotaRepository.findBySocioIdAndPeriodo("socio-1", "2026-08")).thenReturn(Optional.of(agosto));
        when(cuotaRepository.findBySocioIdAndPeriodo("socio-1", "2026-09")).thenReturn(Optional.of(septiembre));
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio));

        RegistrarPagoCuotaRequest request = new RegistrarPagoCuotaRequest(
                "socio-1", List.of("2026-08", "2026-09"), LocalDate.of(2026, 7, 5),
                MedioPago.TRANSFERENCIA, "COMP-1", "paga dos meses juntos");

        RegistrarPagoResponse response = service.registrarPago(request, "admin-1", "Admin Uno");

        assertThat(response.cuotas()).hasSize(2);
        assertThat(response.montoTotal()).isEqualByComparingTo("30000.00");
        assertThat(agosto.getEstado()).isEqualTo(EstadoCuota.PAGADA);
        assertThat(septiembre.getEstado()).isEqualTo(EstadoCuota.PAGADA);
        verify(cuotaRepository).save(agosto);
        verify(cuotaRepository).save(septiembre);
        verify(pagoRepository, times(2)).save(any(Pago.class));
    }

    @Test
    void registrarPago_conCuotasDeDistintoImporte_cadaUnaQuedaConSuPropioImporte() {
        // Regresión: antes se aplicaba un único importe "tipeado" por el admin a
        // todas las cuotas seleccionadas, sin validar contra lo realmente adeudado.
        Cuota agosto = cuotaPendiente("cuota-1", "socio-1", "2026-08");
        Cuota septiembre = cuotaPendiente("cuota-2", "socio-1", "2026-09");
        septiembre.setImporte(new BigDecimal("18000.00"));
        Socio socio = socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO);
        when(cuotaRepository.findBySocioIdAndPeriodo("socio-1", "2026-08")).thenReturn(Optional.of(agosto));
        when(cuotaRepository.findBySocioIdAndPeriodo("socio-1", "2026-09")).thenReturn(Optional.of(septiembre));
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio));

        RegistrarPagoCuotaRequest request = new RegistrarPagoCuotaRequest(
                "socio-1", List.of("2026-08", "2026-09"), LocalDate.of(2026, 7, 5),
                MedioPago.TRANSFERENCIA, "COMP-1", null);

        RegistrarPagoResponse response = service.registrarPago(request, "admin-1", "Admin Uno");

        ArgumentCaptor<Pago> pagoCaptor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository, times(2)).save(pagoCaptor.capture());
        List<BigDecimal> importes = pagoCaptor.getAllValues().stream().map(Pago::getImporte).toList();
        assertThat(importes).containsExactlyInAnyOrder(new BigDecimal("15000.00"), new BigDecimal("18000.00"));
        assertThat(response.montoTotal()).isEqualByComparingTo("33000.00");
    }

    @Test
    void registrarPago_conPeriodoSinCuotaGenerada_lanzaExcepcion() {
        when(cuotaRepository.findBySocioIdAndPeriodo("socio-1", "2026-07")).thenReturn(Optional.empty());

        RegistrarPagoCuotaRequest request = new RegistrarPagoCuotaRequest(
                "socio-1", List.of("2026-07"), LocalDate.of(2026, 7, 5),
                MedioPago.TRANSFERENCIA, null, null);

        assertThatThrownBy(() -> service.registrarPago(request, "admin-1", "Admin Uno"))
                .isInstanceOf(CuotaNoEncontradaException.class);
    }

    // ---------- informarPago ----------

    @Test
    void informarPago_conMedioDistintoDeTransferencia_lanzaExcepcion() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        InformarPagoCuotaRequest request = new InformarPagoCuotaRequest(
                LocalDate.of(2026, 7, 5), new BigDecimal("15000.00"), MedioPago.EFECTIVO, null);

        assertThatThrownBy(() -> service.informarPago("cuota-1", request, comprobantePdf(), "socio-1"))
                .isInstanceOf(CuotaEstadoInvalidoException.class);
    }

    @Test
    void informarPago_sinComprobante_lanzaExcepcion() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        InformarPagoCuotaRequest request = new InformarPagoCuotaRequest(
                LocalDate.of(2026, 7, 5), new BigDecimal("15000.00"), MedioPago.TRANSFERENCIA, null);
        MultipartFile vacio = new MockMultipartFile("comprobante", "comprobante.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.informarPago("cuota-1", request, vacio, "socio-1"))
                .isInstanceOf(ArchivoInvalidoException.class);
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void informarPago_conCuotaPropiaPendiente_pasaAEnRevisionYGuardaElComprobante() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago pago = inv.getArgument(0);
            if (pago.getId() == null) {
                pago.setId("pago-1");
            }
            return pago;
        });
        when(almacenamientoComprobantesService.guardar(eq("pago-1"), any(MultipartFile.class)))
                .thenReturn("pago-1/archivo.pdf");

        InformarPagoCuotaRequest request = new InformarPagoCuotaRequest(
                LocalDate.of(2026, 7, 5), new BigDecimal("15000.00"), MedioPago.TRANSFERENCIA, null);

        InformarPagoResponse response = service.informarPago("cuota-1", request, comprobantePdf(), "socio-1");

        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.EN_REVISION);
        assertThat(response.cuota().estado()).isEqualTo(EstadoCuota.EN_REVISION);
        assertThat(response.cuota().pagoVigente().comprobanteRuta()).isEqualTo("pago-1/archivo.pdf");
        assertThat(response.cuota().pagoVigente().informadoPorSocio()).isTrue();
        verify(pagoRepository, times(2)).save(any(Pago.class));
        // Comprobante como su propia entidad (documento 10.4), además del dato en
        // Pago.comprobanteRuta.
        verify(comprobanteService).registrarSubidoPorSocio(
                any(Pago.class), eq("pago-1/archivo.pdf"), any(MultipartFile.class));
    }

    @Test
    void informarPago_conCuotaPropiaPendiente_avisaATodosLosAdmins() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago pago = inv.getArgument(0);
            if (pago.getId() == null) {
                pago.setId("pago-1");
            }
            return pago;
        });
        when(almacenamientoComprobantesService.guardar(eq("pago-1"), any(MultipartFile.class)))
                .thenReturn("pago-1/archivo.pdf");
        Usuario admin1 = Usuario.builder().id("admin-1").email("admin1@example.com").rol(Rol.ADMIN).build();
        Usuario admin2 = Usuario.builder().id("admin-2").email("admin2@example.com").rol(Rol.ADMIN).build();
        when(usuarioRepository.findByRol(Rol.ADMIN)).thenReturn(List.of(admin1, admin2));

        InformarPagoCuotaRequest request = new InformarPagoCuotaRequest(
                LocalDate.of(2026, 7, 5), new BigDecimal("15000.00"), MedioPago.TRANSFERENCIA, null);

        service.informarPago("cuota-1", request, comprobantePdf(), "socio-1");

        verify(emailService).enviarCorreoPagoInformado(
                eq("admin1@example.com"), eq("SOC-000001"), eq("Lopez, Juan"), eq("2026-07"), any());
        verify(emailService).enviarCorreoPagoInformado(
                eq("admin2@example.com"), eq("SOC-000001"), eq("Lopez, Juan"), eq("2026-07"), any());
    }

    @Test
    void informarPago_deCuotaDeOtroSocio_lanzaCuotaNoEncontrada() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        InformarPagoCuotaRequest request = new InformarPagoCuotaRequest(
                LocalDate.of(2026, 7, 5), new BigDecimal("15000.00"), MedioPago.TRANSFERENCIA, null);

        assertThatThrownBy(() -> service.informarPago("cuota-1", request, comprobantePdf(), "socio-ajeno"))
                .isInstanceOf(CuotaNoEncontradaException.class);
    }

    @Test
    void informarPago_conCuotaEnEstadoInvalido_lanzaExcepcion() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.PAGADA);
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        InformarPagoCuotaRequest request = new InformarPagoCuotaRequest(
                LocalDate.of(2026, 7, 5), new BigDecimal("15000.00"), MedioPago.TRANSFERENCIA, null);

        assertThatThrownBy(() -> service.informarPago("cuota-1", request, comprobantePdf(), "socio-1"))
                .isInstanceOf(CuotaEstadoInvalidoException.class);
    }

    @Test
    void informarPago_conCuotaRechazada_permiteReintentar() {
        // RN-17: un rechazo previo (Pago propio, no se toca) no deja la cuota
        // trabada para siempre.
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.RECHAZADA);
        cuota.setMotivoRechazo("Comprobante ilegible");
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        InformarPagoCuotaRequest request = new InformarPagoCuotaRequest(
                LocalDate.of(2026, 7, 5), new BigDecimal("15000.00"), MedioPago.TRANSFERENCIA, null);

        InformarPagoResponse response = service.informarPago("cuota-1", request, comprobantePdf(), "socio-1");

        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.EN_REVISION);
        assertThat(cuota.getMotivoRechazo()).isNull();
        assertThat(response.cuota().estado()).isEqualTo(EstadoCuota.EN_REVISION);
    }

    // ---------- revisarPagoInformado ----------

    @Test
    void revisarPagoInformado_aprobar_marcaPagada() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.EN_REVISION);
        Pago pago = Pago.builder().id("pago-1").cuotaId("cuota-1")
                .importe(new BigDecimal("15000.00")).estado(EstadoPago.EN_REVISION).informadoPorSocio(true).build();
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(pagoRepository.findByCuotaId("cuota-1")).thenReturn(List.of(pago));
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO)));

        RevisarPagoInformadoResponse response = service.revisarPagoInformado(
                "cuota-1", new RevisarPagoInformadoRequest(true, null), "admin-1", "Admin Uno");

        assertThat(response.mensaje()).isEqualTo("Pago aprobado con éxito");
        assertThat(response.cuota().estado()).isEqualTo(EstadoCuota.PAGADA);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(pago.getRegistradoPorAdminNombre()).isEqualTo("Admin Uno");
        verify(emailService).enviarCorreoPagoRegistrado(anyString(), anyString(), anyString(), any());
    }

    @Test
    void revisarPagoInformado_rechazarSinMotivo_lanzaExcepcion() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.EN_REVISION);
        Pago pago = Pago.builder().id("pago-1").cuotaId("cuota-1").estado(EstadoPago.EN_REVISION).build();
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(pagoRepository.findByCuotaId("cuota-1")).thenReturn(List.of(pago));

        assertThatThrownBy(() -> service.revisarPagoInformado(
                "cuota-1", new RevisarPagoInformadoRequest(false, null), "admin-1", "Admin Uno"))
                .isInstanceOf(CuotaEstadoInvalidoException.class);
    }

    @Test
    void revisarPagoInformado_rechazarConMotivo_marcaRechazada() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.EN_REVISION);
        Pago pago = Pago.builder().id("pago-1").cuotaId("cuota-1").estado(EstadoPago.EN_REVISION).build();
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(pagoRepository.findByCuotaId("cuota-1")).thenReturn(List.of(pago));
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO)));

        RevisarPagoInformadoResponse response = service.revisarPagoInformado(
                "cuota-1", new RevisarPagoInformadoRequest(false, "Comprobante ilegible"), "admin-1", "Admin Uno");

        assertThat(response.mensaje()).isEqualTo("Pago rechazado con éxito");
        assertThat(response.cuota().estado()).isEqualTo(EstadoCuota.RECHAZADA);
        assertThat(response.cuota().motivoRechazo()).isEqualTo("Comprobante ilegible");
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.RECHAZADO);
        // Un Pago recién RECHAZADO no es "vigente" (ver el contrato de
        // CuotaResponse.pagoVigente: vigente = APROBADO o EN_REVISION); el motivo
        // del rechazo se ve igual en CuotaResponse.motivoRechazo.
        assertThat(response.cuota().pagoVigente()).isNull();
        verify(emailService).enviarCorreoPagoRechazado(anyString(), anyString(), anyString(), eq("Comprobante ilegible"));
    }

    @Test
    void revisarPagoInformado_conUnLinkDePagoAbandonadoDeLaMismaCuota_revisaLaTransferenciaMasReciente() {
        // Regresión: antes de este fix, si había más de un Pago EN_REVISION para la
        // misma cuota (un link de pago abandonado sin resolver + la transferencia
        // recién informada), esto tiraba IncorrectResultSizeDataAccessException
        // porque se buscaba con un findByCuotaIdAndEstado que devuelve Optional.
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.EN_REVISION);
        Instant hace2Dias = Instant.now().minusSeconds(172_800);
        Pago linkAbandonado = Pago.builder().id("pago-viejo").cuotaId("cuota-1")
                .medioPago(MedioPago.LINK_DE_PAGO).estado(EstadoPago.EN_REVISION).fechaCreacion(hace2Dias).build();
        Pago transferenciaInformada = Pago.builder().id("pago-nuevo").cuotaId("cuota-1")
                .importe(new BigDecimal("15000.00")).medioPago(MedioPago.TRANSFERENCIA)
                .estado(EstadoPago.EN_REVISION).fechaCreacion(Instant.now()).build();
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(pagoRepository.findByCuotaId("cuota-1")).thenReturn(List.of(linkAbandonado, transferenciaInformada));
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO)));

        RevisarPagoInformadoResponse response = service.revisarPagoInformado(
                "cuota-1", new RevisarPagoInformadoRequest(true, null), "admin-1", "Admin Uno");

        assertThat(response.cuota().estado()).isEqualTo(EstadoCuota.PAGADA);
        // Se aprobó la transferencia (la más reciente), no el link abandonado.
        assertThat(transferenciaInformada.getEstado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(linkAbandonado.getEstado()).isEqualTo(EstadoPago.EN_REVISION);
    }

    @Test
    void revisarPagoInformado_cuotaNoEnRevision_lanzaExcepcion() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        assertThatThrownBy(() -> service.revisarPagoInformado(
                "cuota-1", new RevisarPagoInformadoRequest(true, null), "admin-1", "Admin Uno"))
                .isInstanceOf(CuotaEstadoInvalidoException.class);
    }

    // ---------- generarLinkDePago (Mercado Pago) ----------

    @Test
    void generarLinkDePago_conCuotaPropiaPendiente_creaPreferenciaYDevuelveElInitPoint() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago pago = inv.getArgument(0);
            if (pago.getId() == null) {
                pago.setId("pago-1");
            }
            return pago;
        });
        when(mercadoPagoService.crearPreferencia(eq("pago-1"), anyString(), eq(new BigDecimal("15000.00"))))
                .thenReturn(new PreferenciaMercadoPago("pref-1", "https://mercadopago.com/checkout/pref-1"));

        LinkDePagoResponse response = service.generarLinkDePago("cuota-1", "socio-1");

        assertThat(response.pagoId()).isEqualTo("pago-1");
        assertThat(response.linkDePago()).isEqualTo("https://mercadopago.com/checkout/pref-1");
        // Crear la preferencia no bloquea la cuota: recién se bloquea cuando Mercado
        // Pago confirma por webhook que hay un intento real en curso (ver
        // procesarNotificacionMercadoPago), no al abrir el link.
        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.PENDIENTE);
        verify(pagoRepository, times(2)).save(any(Pago.class));
        verify(cuotaRepository, never()).save(any());
    }

    @Test
    void generarLinkDePago_deCuotaDeOtroSocio_lanzaCuotaNoEncontrada() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        assertThatThrownBy(() -> service.generarLinkDePago("cuota-1", "socio-ajeno"))
                .isInstanceOf(CuotaNoEncontradaException.class);
    }

    @Test
    void generarLinkDePago_conCuotaYaPagada_lanzaExcepcion() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.PAGADA);
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        assertThatThrownBy(() -> service.generarLinkDePago("cuota-1", "socio-1"))
                .isInstanceOf(CuotaEstadoInvalidoException.class);
    }

    @Test
    void generarLinkDePago_conCuotaRechazada_permiteReintentar() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.RECHAZADA);
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago pago = inv.getArgument(0);
            if (pago.getId() == null) {
                pago.setId("pago-2");
            }
            return pago;
        });
        when(mercadoPagoService.crearPreferencia(eq("pago-2"), anyString(), any(BigDecimal.class)))
                .thenReturn(new PreferenciaMercadoPago("pref-2", "https://mercadopago.com/checkout/pref-2"));

        LinkDePagoResponse response = service.generarLinkDePago("cuota-1", "socio-1");

        assertThat(response.linkDePago()).isEqualTo("https://mercadopago.com/checkout/pref-2");
        // Igual que en el caso PENDIENTE: generar el link no cambia el estado de la
        // cuota, sigue RECHAZADA (visible el motivo del intento anterior) hasta que
        // Mercado Pago confirme algo nuevo sobre este intento.
        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.RECHAZADA);
        verify(cuotaRepository, never()).save(any());
    }

    @Test
    void generarLinkDePago_abandonadoSinRespuestaDeMercadoPago_noDejaLaCuotaTrabada() {
        // Caracterización del bug reportado por el front: el socio abre el link y
        // cierra la pestaña sin pagar. Nunca llega ningún webhook (no hay pago real
        // que consultar), así que la cuota debe seguir admitiendo un nuevo intento.
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago pago = inv.getArgument(0);
            if (pago.getId() == null) {
                pago.setId("pago-1");
            }
            return pago;
        });
        when(mercadoPagoService.crearPreferencia(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(new PreferenciaMercadoPago("pref-1", "https://mercadopago.com/checkout/pref-1"));

        service.generarLinkDePago("cuota-1", "socio-1");

        // Sin ningún webhook de por medio, la cuota nunca se tocó: sigue admitiendo
        // un nuevo link (o una transferencia) sin que el socio quede bloqueado.
        assertThatCode(() -> service.generarLinkDePago("cuota-1", "socio-1")).doesNotThrowAnyException();
    }

    // ---------- procesarNotificacionMercadoPago (webhook) ----------

    @Test
    void procesarNotificacionMercadoPago_aprobado_marcaCuotaPagada() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.EN_REVISION);
        Pago pago = Pago.builder().id("pago-1").cuotaId("cuota-1").socioId("socio-1")
                .estado(EstadoPago.EN_REVISION).medioPago(MedioPago.LINK_DE_PAGO).build();
        when(mercadoPagoService.consultarPago("mp-pago-1"))
                .thenReturn(new EstadoPagoMercadoPago("mp-pago-1", "approved", "pago-1"));
        when(pagoRepository.findById("pago-1")).thenReturn(Optional.of(pago));
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO)));

        service.procesarNotificacionMercadoPago("mp-pago-1");

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(pago.getMercadoPagoPaymentId()).isEqualTo("mp-pago-1");
        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.PAGADA);
        verify(emailService).enviarCorreoPagoRegistrado(anyString(), anyString(), anyString(), any());
    }

    @Test
    void procesarNotificacionMercadoPago_rechazado_marcaCuotaRechazadaYPermiteReintento() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.EN_REVISION);
        Pago pago = Pago.builder().id("pago-1").cuotaId("cuota-1").socioId("socio-1")
                .estado(EstadoPago.EN_REVISION).medioPago(MedioPago.LINK_DE_PAGO).build();
        when(mercadoPagoService.consultarPago("mp-pago-1"))
                .thenReturn(new EstadoPagoMercadoPago("mp-pago-1", "rejected", "pago-1"));
        when(pagoRepository.findById("pago-1")).thenReturn(Optional.of(pago));
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO)));

        service.procesarNotificacionMercadoPago("mp-pago-1");

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.RECHAZADA);
        assertThat(cuota.getMotivoRechazo()).isEqualTo("Pago rechazado por Mercado Pago");
        verify(emailService).enviarCorreoPagoRechazado(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void procesarNotificacionMercadoPago_aprobadoConCuotaYaPagadaPorOtroIntento_noPisaLaCuota() {
        // Doble intento en paralelo (dos links para la misma cuota): el primero ya
        // la pagó. La aprobación tardía del segundo no debe volver a "aprobar" nada
        // ni reenviar el correo de pago registrado (posible pago duplicado a
        // resolver a mano, no algo que el sistema deba intentar arreglar solo).
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.PAGADA);
        Pago pago = Pago.builder().id("pago-2").cuotaId("cuota-1").socioId("socio-1")
                .estado(EstadoPago.EN_REVISION).medioPago(MedioPago.LINK_DE_PAGO).build();
        when(mercadoPagoService.consultarPago("mp-pago-2"))
                .thenReturn(new EstadoPagoMercadoPago("mp-pago-2", "approved", "pago-2"));
        when(pagoRepository.findById("pago-2")).thenReturn(Optional.of(pago));
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        service.procesarNotificacionMercadoPago("mp-pago-2");

        // El Pago en sí sigue quedando aprobado (es verdad, Mercado Pago lo aprobó):
        // lo que no hacemos es tocar la cuota, que otro intento ya había resuelto.
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.PAGADA);
        verify(cuotaRepository, never()).save(any());
        verify(emailService, never()).enviarCorreoPagoRegistrado(anyString(), anyString(), anyString(), any());
    }

    @Test
    void procesarNotificacionMercadoPago_rechazadoConCuotaYaPagadaPorOtroIntento_noLaReabre() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.PAGADA);
        Pago pago = Pago.builder().id("pago-2").cuotaId("cuota-1").socioId("socio-1")
                .estado(EstadoPago.EN_REVISION).medioPago(MedioPago.LINK_DE_PAGO).build();
        when(mercadoPagoService.consultarPago("mp-pago-2"))
                .thenReturn(new EstadoPagoMercadoPago("mp-pago-2", "rejected", "pago-2"));
        when(pagoRepository.findById("pago-2")).thenReturn(Optional.of(pago));
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        service.procesarNotificacionMercadoPago("mp-pago-2");

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.PAGADA);
        verify(cuotaRepository, never()).save(any());
        verify(emailService, never()).enviarCorreoPagoRechazado(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void procesarNotificacionMercadoPago_pendienteDesdeCuotaPendiente_pasaAEnRevision() {
        // Acá (y no al generar el link) es donde la cuota se bloquea de verdad: recién
        // cuando Mercado Pago confirma que hay un intento real en curso.
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        Pago pago = Pago.builder().id("pago-1").cuotaId("cuota-1").socioId("socio-1")
                .estado(EstadoPago.EN_REVISION).medioPago(MedioPago.LINK_DE_PAGO).build();
        when(mercadoPagoService.consultarPago("mp-pago-1"))
                .thenReturn(new EstadoPagoMercadoPago("mp-pago-1", "pending", "pago-1"));
        when(pagoRepository.findById("pago-1")).thenReturn(Optional.of(pago));
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        service.procesarNotificacionMercadoPago("mp-pago-1");

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.EN_REVISION);
        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.EN_REVISION);
        // Regresión: un pago "pending" todavía no se pagó de verdad, así que
        // fechaPago no debe fijarse hasta que el resultado sea definitivo.
        assertThat(pago.getFechaPago()).isNull();
        assertThat(pago.getMercadoPagoPaymentId()).isEqualTo("mp-pago-1");
        verify(cuotaRepository, times(1)).save(cuota);
    }

    @Test
    void procesarNotificacionMercadoPago_pendienteConCuotaYaEnRevision_noGuardaDeNuevo() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.EN_REVISION);
        Pago pago = Pago.builder().id("pago-1").cuotaId("cuota-1").socioId("socio-1")
                .estado(EstadoPago.EN_REVISION).medioPago(MedioPago.LINK_DE_PAGO).build();
        when(mercadoPagoService.consultarPago("mp-pago-1"))
                .thenReturn(new EstadoPagoMercadoPago("mp-pago-1", "pending", "pago-1"));
        when(pagoRepository.findById("pago-1")).thenReturn(Optional.of(pago));
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        service.procesarNotificacionMercadoPago("mp-pago-1");

        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.EN_REVISION);
        verify(cuotaRepository, never()).save(any());
    }

    @Test
    void procesarNotificacionMercadoPago_pendienteConCuotaYaPagadaPorOtroIntento_noLaReabre() {
        // Caso límite: otro Pago en paralelo (otro link para la misma cuota) ya la
        // había pagado; una notificación "pending" tardía de este intento no debe
        // volver a poner la cuota en revisión.
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.PAGADA);
        Pago pago = Pago.builder().id("pago-1").cuotaId("cuota-1").socioId("socio-1")
                .estado(EstadoPago.EN_REVISION).medioPago(MedioPago.LINK_DE_PAGO).build();
        when(mercadoPagoService.consultarPago("mp-pago-1"))
                .thenReturn(new EstadoPagoMercadoPago("mp-pago-1", "pending", "pago-1"));
        when(pagoRepository.findById("pago-1")).thenReturn(Optional.of(pago));
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        service.procesarNotificacionMercadoPago("mp-pago-1");

        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.PAGADA);
        verify(cuotaRepository, never()).save(any());
    }

    @Test
    void procesarNotificacionMercadoPago_notificacionRepetidaDePagoYaResuelto_esIdempotente() {
        Pago pagoYaAprobado = Pago.builder().id("pago-1").cuotaId("cuota-1").estado(EstadoPago.APROBADO).build();
        when(mercadoPagoService.consultarPago("mp-pago-1"))
                .thenReturn(new EstadoPagoMercadoPago("mp-pago-1", "approved", "pago-1"));
        when(pagoRepository.findById("pago-1")).thenReturn(Optional.of(pagoYaAprobado));

        service.procesarNotificacionMercadoPago("mp-pago-1");

        verify(pagoRepository, never()).save(any());
        verify(cuotaRepository, never()).save(any());
    }

    @Test
    void procesarNotificacionMercadoPago_sinExternalReference_seIgnora() {
        when(mercadoPagoService.consultarPago("mp-pago-1"))
                .thenReturn(new EstadoPagoMercadoPago("mp-pago-1", "approved", null));

        service.procesarNotificacionMercadoPago("mp-pago-1");

        verify(pagoRepository, never()).findById(anyString());
    }

    @Test
    void procesarNotificacionMercadoPago_pagoInexistente_seIgnora() {
        when(mercadoPagoService.consultarPago("mp-pago-1"))
                .thenReturn(new EstadoPagoMercadoPago("mp-pago-1", "approved", "pago-inexistente"));
        when(pagoRepository.findById("pago-inexistente")).thenReturn(Optional.empty());

        service.procesarNotificacionMercadoPago("mp-pago-1");

        verify(cuotaRepository, never()).save(any());
    }

    // ---------- anularCuota ----------

    @Test
    void anularCuota_conCuotaPendiente_marcaAnulada() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        CuotaResponse response = service.anularCuota(
                "cuota-1", new AnularCuotaRequest("Se generó por error"), "admin-1", "Admin Uno");

        assertThat(response.estado()).isEqualTo(EstadoCuota.ANULADA);
        assertThat(response.motivoAnulacion()).isEqualTo("Se generó por error");
    }

    @Test
    void anularCuota_conCuotaYaPagada_lanzaExcepcion() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.PAGADA);
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        assertThatThrownBy(() -> service.anularCuota(
                "cuota-1", new AnularCuotaRequest("motivo"), "admin-1", "Admin Uno"))
                .isInstanceOf(CuotaEstadoInvalidoException.class);
    }

    // ---------- estado de cuenta ----------

    @Test
    void obtenerEstadoCuentaSocio_sumaSoloEstadosQueCuentanComoDeuda() {
        Socio socio = socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO);
        Cuota pendiente = cuotaPendiente("cuota-1", "socio-1");
        Cuota pagada = cuotaPendiente("cuota-2", "socio-1");
        pagada.setEstado(EstadoCuota.PAGADA);
        Cuota anulada = cuotaPendiente("cuota-3", "socio-1");
        anulada.setEstado(EstadoCuota.ANULADA);

        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio));
        when(cuotaRepository.findBySocioId("socio-1")).thenReturn(List.of(pendiente, pagada, anulada));

        EstadoCuentaSocioResponse response = service.obtenerEstadoCuentaSocio("socio-1");

        assertThat(response.deudaTotal()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(response.cuotas()).hasSize(3);
    }

    @Test
    void obtenerEstadoCuentaSocio_socioInexistente_lanzaExcepcion() {
        when(socioRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerEstadoCuentaSocio("no-existe"))
                .isInstanceOf(SocioNoEncontradoException.class);
    }

    // ---------- job de vencimiento ----------

    @Test
    void marcarCuotasVencidas_marcaLasQuePasaronLaFechaDeVencimiento() {
        Cuota vencida = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findByEstadoAndFechaVencimientoBefore(eq(EstadoCuota.PENDIENTE), any(LocalDate.class)))
                .thenReturn(List.of(vencida));

        service.marcarCuotasVencidas();

        assertThat(vencida.getEstado()).isEqualTo(EstadoCuota.VENCIDA);
        verify(cuotaRepository).save(vencida);
    }

    // ---------- enviarRecordatoriosDeCuotas ----------

    @Test
    void enviarRecordatoriosDeCuotas_avisaACincoYUnDiaYElDiaDeVencimiento() {
        Socio socio = socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO);
        when(socioRepository.findAllById(anyList())).thenReturn(List.of(socio));

        Cuota aCincoDias = cuotaPendiente("cuota-5", "socio-1");
        Cuota aUnDia = cuotaPendiente("cuota-1", "socio-1");
        Cuota hoy = cuotaPendiente("cuota-0", "socio-1");
        LocalDate hoyFecha = LocalDate.now();
        when(cuotaRepository.findByEstadoAndFechaVencimiento(EstadoCuota.PENDIENTE, hoyFecha.plusDays(5)))
                .thenReturn(List.of(aCincoDias));
        when(cuotaRepository.findByEstadoAndFechaVencimiento(EstadoCuota.PENDIENTE, hoyFecha.plusDays(1)))
                .thenReturn(List.of(aUnDia));
        when(cuotaRepository.findByEstadoAndFechaVencimiento(EstadoCuota.PENDIENTE, hoyFecha))
                .thenReturn(List.of(hoy));
        when(cuotaRepository.findByEstado(EstadoCuota.VENCIDA)).thenReturn(List.of());

        service.enviarRecordatoriosDeCuotas();

        verify(emailService).enviarCorreoCuotaProximaAVencer(
                eq("juan@example.com"), anyString(), anyString(), any(), any(), eq(5));
        verify(emailService).enviarCorreoCuotaProximaAVencer(
                eq("juan@example.com"), anyString(), anyString(), any(), any(), eq(1));
        verify(emailService).enviarCorreoCuotaProximaAVencer(
                eq("juan@example.com"), anyString(), anyString(), any(), any(), eq(0));
        // D-6: un findAllById por cada día con cuotas (3 buckets no vacíos), nunca findById por cuota.
        verify(socioRepository, times(3)).findAllById(anyList());
        verify(socioRepository, never()).findById(anyString());
    }

    @Test
    void enviarRecordatorioPorDiasRestantes_variasCuotasEnElMismoDia_respetaOrdenOriginalDeLaLista() {
        // Caracterización previa a D-6 (findAllById + Map): confirma que el orden de
        // envío sigue el orden de la lista devuelta por el repositorio (no un orden
        // alfabético/por id), para poder detectar si el futuro Map rompe ese orden.
        Socio socioB = socioActivo("socio-b", "SOC-000002", CategoriaSocio.ACTIVO, "b@example.com");
        Socio socioA = socioActivo("socio-a", "SOC-000001", CategoriaSocio.ACTIVO, "a@example.com");
        when(socioRepository.findAllById(anyList())).thenReturn(List.of(socioB, socioA));

        Cuota cuotaB = cuotaPendiente("cuota-b", "socio-b");
        Cuota cuotaA = cuotaPendiente("cuota-a", "socio-a");
        LocalDate hoyFecha = LocalDate.now();
        // Orden deliberado: socio-b antes que socio-a en la lista devuelta.
        when(cuotaRepository.findByEstadoAndFechaVencimiento(EstadoCuota.PENDIENTE, hoyFecha.plusDays(5)))
                .thenReturn(List.of(cuotaB, cuotaA));
        when(cuotaRepository.findByEstadoAndFechaVencimiento(EstadoCuota.PENDIENTE, hoyFecha.plusDays(1)))
                .thenReturn(List.of());
        when(cuotaRepository.findByEstadoAndFechaVencimiento(EstadoCuota.PENDIENTE, hoyFecha))
                .thenReturn(List.of());
        when(cuotaRepository.findByEstado(EstadoCuota.VENCIDA)).thenReturn(List.of());

        service.enviarRecordatoriosDeCuotas();

        InOrder orden = inOrder(emailService);
        orden.verify(emailService).enviarCorreoCuotaProximaAVencer(
                eq("b@example.com"), anyString(), anyString(), any(), any(), eq(5));
        orden.verify(emailService).enviarCorreoCuotaProximaAVencer(
                eq("a@example.com"), anyString(), anyString(), any(), any(), eq(5));
        verify(emailService, times(2)).enviarCorreoCuotaProximaAVencer(
                anyString(), anyString(), anyString(), any(), any(), eq(5));
        // D-6: un único findAllById para el bucket de día 5 (2 cuotas, 1 query), buckets vacíos no llaman nada.
        verify(socioRepository, times(1)).findAllById(anyList());
        verify(socioRepository, never()).findById(anyString());
    }

    @Test
    void enviarRecordatoriosDeCuotas_avisaDeDeudaElPrimerDiaVencidaYCadaSieteDiasDespues() {
        Socio socio = socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO);
        when(socioRepository.findAllById(anyList())).thenReturn(List.of(socio));
        when(cuotaRepository.findByEstadoAndFechaVencimiento(eq(EstadoCuota.PENDIENTE), any(LocalDate.class)))
                .thenReturn(List.of());

        Cuota vencidaAyer = cuotaPendiente("cuota-1", "socio-1");
        vencidaAyer.setEstado(EstadoCuota.VENCIDA);
        vencidaAyer.setFechaVencimiento(LocalDate.now().minusDays(1));

        Cuota vencidaHaceSieteDias = cuotaPendiente("cuota-2", "socio-1");
        vencidaHaceSieteDias.setEstado(EstadoCuota.VENCIDA);
        vencidaHaceSieteDias.setFechaVencimiento(LocalDate.now().minusDays(7));

        Cuota vencidaHaceCatorceDias = cuotaPendiente("cuota-4", "socio-1");
        vencidaHaceCatorceDias.setEstado(EstadoCuota.VENCIDA);
        vencidaHaceCatorceDias.setFechaVencimiento(LocalDate.now().minusDays(14));

        Cuota vencidaHaceTresDias = cuotaPendiente("cuota-3", "socio-1");
        vencidaHaceTresDias.setEstado(EstadoCuota.VENCIDA);
        vencidaHaceTresDias.setFechaVencimiento(LocalDate.now().minusDays(3));

        when(cuotaRepository.findByEstado(EstadoCuota.VENCIDA))
                .thenReturn(List.of(vencidaAyer, vencidaHaceSieteDias, vencidaHaceCatorceDias, vencidaHaceTresDias));

        service.enviarRecordatoriosDeCuotas();

        // Día 1, día 7 y día 14 sí avisan; día 3 (ni el primer día ni múltiplo de 7) no.
        verify(emailService, times(3)).enviarCorreoCuotaVencida(
                eq("juan@example.com"), anyString(), anyString(), any(), any());
        // D-6: un único findAllById con el id deduplicado (mismo socio en las 3 cuotas que avisan).
        verify(socioRepository, times(1)).findAllById(anyList());
        verify(socioRepository, never()).findById(anyString());
    }

    @Test
    void enviarAvisosDeDeuda_variasCuotasVencidas_respetaOrdenOriginalDeLaLista() {
        // Caracterización previa a D-6: mismo objetivo que el test análogo de
        // enviarRecordatorioPorDiasRestantes, pero para el loop de avisos de deuda.
        Socio socioB = socioActivo("socio-b", "SOC-000002", CategoriaSocio.ACTIVO, "b@example.com");
        Socio socioA = socioActivo("socio-a", "SOC-000001", CategoriaSocio.ACTIVO, "a@example.com");
        when(socioRepository.findAllById(anyList())).thenReturn(List.of(socioB, socioA));
        when(cuotaRepository.findByEstadoAndFechaVencimiento(eq(EstadoCuota.PENDIENTE), any(LocalDate.class)))
                .thenReturn(List.of());

        Cuota vencidaB = cuotaPendiente("cuota-b", "socio-b");
        vencidaB.setEstado(EstadoCuota.VENCIDA);
        vencidaB.setFechaVencimiento(LocalDate.now().minusDays(1));
        Cuota vencidaA = cuotaPendiente("cuota-a", "socio-a");
        vencidaA.setEstado(EstadoCuota.VENCIDA);
        vencidaA.setFechaVencimiento(LocalDate.now().minusDays(1));

        // Orden deliberado: socio-b antes que socio-a en la lista devuelta.
        when(cuotaRepository.findByEstado(EstadoCuota.VENCIDA)).thenReturn(List.of(vencidaB, vencidaA));

        service.enviarRecordatoriosDeCuotas();

        InOrder orden = inOrder(emailService);
        orden.verify(emailService).enviarCorreoCuotaVencida(eq("b@example.com"), anyString(), anyString(), any(), any());
        orden.verify(emailService).enviarCorreoCuotaVencida(eq("a@example.com"), anyString(), anyString(), any(), any());
        // D-6: un único findAllById para ambas cuotas vencidas del mismo día.
        verify(socioRepository, times(1)).findAllById(anyList());
        verify(socioRepository, never()).findById(anyString());
    }

    // ---------- resumen ----------

    @Test
    void obtenerResumen_calculaTotalesYCantidadesPorEstado() {
        Cuota pagadaEfectivo = cuotaPendiente("cuota-1", "socio-1");
        pagadaEfectivo.setEstado(EstadoCuota.PAGADA);

        Cuota pagadaTransferencia = cuotaPendiente("cuota-2", "socio-2");
        pagadaTransferencia.setEstado(EstadoCuota.PAGADA);

        Cuota enRevision = cuotaPendiente("cuota-3", "socio-3");
        enRevision.setEstado(EstadoCuota.EN_REVISION);

        Cuota pendiente = cuotaPendiente("cuota-4", "socio-4");

        Cuota vencida = cuotaPendiente("cuota-5", "socio-5");
        vencida.setEstado(EstadoCuota.VENCIDA);

        Cuota rechazada = cuotaPendiente("cuota-6", "socio-6");
        rechazada.setEstado(EstadoCuota.RECHAZADA);

        when(cuotaRepository.findAll()).thenReturn(List.of(
                pagadaEfectivo, pagadaTransferencia, enRevision, pendiente, vencida, rechazada));
        when(pagoRepository.findByEstado(EstadoPago.APROBADO)).thenReturn(List.of(
                pagoAprobado("pago-1", "cuota-1", "15000.00", MedioPago.EFECTIVO),
                pagoAprobado("pago-2", "cuota-2", "10000.00", MedioPago.TRANSFERENCIA)));

        ResumenCuotasResponse response = service.obtenerResumen();

        assertThat(response.totalCobrado()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(response.totalCobradoEnEfectivo()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(response.totalEnRevision()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(response.cantidadTodas()).isEqualTo(6);
        assertThat(response.cantidadPendientes()).isEqualTo(3); // pendiente + vencida + en_revision
        assertThat(response.cantidadAprobadas()).isEqualTo(2);
        assertThat(response.cantidadRechazadas()).isEqualTo(1);

        // cuotaPendiente() por defecto genera todo con categoria ACTIVO: todo el
        // total cobrado y las 6 cuotas caen en esa fila, ADHERENTE queda en cero.
        assertThat(response.cobranzaPorCategoria()).hasSize(2);
        var porCategoriaActivo = response.cobranzaPorCategoria().stream()
                .filter(c -> c.categoria() == CategoriaSocio.ACTIVO).findFirst().orElseThrow();
        var porCategoriaAdherente = response.cobranzaPorCategoria().stream()
                .filter(c -> c.categoria() == CategoriaSocio.ADHERENTE).findFirst().orElseThrow();
        assertThat(porCategoriaActivo.totalCobrado()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(porCategoriaActivo.cantidadCuotas()).isEqualTo(6);
        assertThat(porCategoriaAdherente.totalCobrado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(porCategoriaAdherente.cantidadCuotas()).isZero();
    }

    @Test
    void obtenerResumen_conCuotasDeAmbasCategorias_desglosaCorrectamente() {
        Cuota pagadaActivo = cuotaPendiente("cuota-1", "socio-1");
        pagadaActivo.setEstado(EstadoCuota.PAGADA);
        pagadaActivo.setCategoria(CategoriaSocio.ACTIVO);

        Cuota pagadaAdherente = cuotaPendiente("cuota-2", "socio-2");
        pagadaAdherente.setEstado(EstadoCuota.PAGADA);
        pagadaAdherente.setCategoria(CategoriaSocio.ADHERENTE);

        Cuota pendienteAdherente = cuotaPendiente("cuota-3", "socio-3");
        pendienteAdherente.setCategoria(CategoriaSocio.ADHERENTE);

        when(cuotaRepository.findAll()).thenReturn(List.of(pagadaActivo, pagadaAdherente, pendienteAdherente));
        when(pagoRepository.findByEstado(EstadoPago.APROBADO)).thenReturn(List.of(
                pagoAprobado("pago-1", "cuota-1", "15000.00", MedioPago.EFECTIVO),
                pagoAprobado("pago-2", "cuota-2", "8000.00", MedioPago.TRANSFERENCIA)));

        ResumenCuotasResponse response = service.obtenerResumen();

        var porCategoriaActivo = response.cobranzaPorCategoria().stream()
                .filter(c -> c.categoria() == CategoriaSocio.ACTIVO).findFirst().orElseThrow();
        var porCategoriaAdherente = response.cobranzaPorCategoria().stream()
                .filter(c -> c.categoria() == CategoriaSocio.ADHERENTE).findFirst().orElseThrow();

        assertThat(porCategoriaActivo.totalCobrado()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(porCategoriaActivo.cantidadCuotas()).isEqualTo(1);
        assertThat(porCategoriaAdherente.totalCobrado()).isEqualByComparingTo(new BigDecimal("8000.00"));
        assertThat(porCategoriaAdherente.cantidadCuotas()).isEqualTo(2);
    }

    // ---------- generarCuotas: Mongo real (Testcontainers) ----------

    /**
     * Suite aparte con Mongo real: un mock de Mockito no puede probar de forma
     * confiable (a) que Spring Data efectivamente lanza
     * IncorrectResultSizeDataAccessException cuando hay dos ReglaCuota para la
     * misma categoría (comportamiento del derived query, no de nuestro código),
     * ni (b) una foto record-level de lo que generarCuotas persiste con una
     * semilla fija, para comparar antes/después del refactor D-4/D-5 (fase 4).
     *
     * No reutiliza com.almoby.ruralcuruzu.TestcontainersConfiguration (es
     * package-private en otro paquete): arranca su propio MongoDBContainer y
     * apunta app.mongodb.uri directo a él vía @DynamicPropertySource, el mismo
     * punto de extensión que ya usa MongoConfig — self-contained acá para no
     * tocar ningún archivo fuera del alcance de este cambio.
     */
    @Nested
    @Testcontainers
    @TestPropertySource(properties = {
            "jwt.secret=test-only-secret-do-not-use-in-production-1234567890"
    })
    @SpringBootTest
    class GenerarCuotasConMongoRealTest {

        @Container
        private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer(DockerImageName.parse("mongo:latest"));

        @DynamicPropertySource
        static void mongoProperties(DynamicPropertyRegistry registry) {
            registry.add("app.mongodb.uri", () -> MONGO_DB_CONTAINER.getReplicaSetUrl("test"));
        }

        @Autowired
        private CuotaServiceImpl cuotaServiceReal;
        // Spies (no mocks/stubs) para verificar cantidad de invocaciones sobre Mongo
        // real (D-4/D-5: findAll()/findByPeriodo() una vez, no por-socio) sin perder
        // el comportamiento real del repositorio.
        @MockitoSpyBean
        private CuotaRepository cuotaRepositoryReal;
        @Autowired
        private SocioRepository socioRepositoryReal;
        @MockitoSpyBean
        private ReglaCuotaRepository reglaCuotaRepositoryReal;
        @Autowired
        private EjecucionGeneracionCuotasRepository ejecucionRepositoryReal;

        @MockitoBean
        private EmailService emailServiceReal;

        @BeforeEach
        void limpiarColecciones() {
            cuotaRepositoryReal.deleteAll();
            socioRepositoryReal.deleteAll();
            reglaCuotaRepositoryReal.deleteAll();
            ejecucionRepositoryReal.deleteAll();
        }

        private ReglaCuota reglaCuotaReal(CategoriaSocio categoria, String nombre, String importe, int diaVencimiento) {
            return ReglaCuota.builder()
                    .categoriaAplicable(categoria)
                    .nombre(nombre)
                    .importe(new BigDecimal(importe))
                    .diaVencimiento(diaVencimiento)
                    .build();
        }

        private Socio socioActivoReal(String numeroSocio, CategoriaSocio categoria, String email) {
            DatosPersonaFisica datos = new DatosPersonaFisica(
                    "Lopez, Juan", "12345678", null, null, null, null, null, email, null, null);
            return Socio.builder()
                    .numeroSocio(numeroSocio)
                    .categoria(categoria)
                    .tipoPersona(TipoPersona.FISICA)
                    .datosPersonaFisica(datos)
                    .estado(EstadoSocio.ACTIVO)
                    .build();
        }

        @Test
        void generarCuotas_conDosReglasParaLaMismaCategoriaYUnSocioDeEsaCategoria_lanzaIncorrectResultSize() {
            reglaCuotaRepositoryReal.save(reglaCuotaReal(CategoriaSocio.ACTIVO, "Regla A", "10000.00", 10));
            reglaCuotaRepositoryReal.save(reglaCuotaReal(CategoriaSocio.ACTIVO, "Regla B", "12000.00", 15));
            socioRepositoryReal.save(socioActivoReal("SOC-000001", CategoriaSocio.ACTIVO, "socio@example.com"));

            assertThatThrownBy(() -> cuotaServiceReal.generarCuotas("2026-07", null, null))
                    .isInstanceOf(IncorrectResultSizeDataAccessException.class);

            // D-4: el preload es un único findAll(), nunca findByCategoriaAplicable por socio.
            verify(reglaCuotaRepositoryReal, times(1)).findAll();
            verify(reglaCuotaRepositoryReal, never()).findByCategoriaAplicable(any());
        }

        @Test
        void generarCuotas_conDosReglasDuplicadasPeroSinSocioDeEsaCategoria_noLanzaExcepcion() {
            reglaCuotaRepositoryReal.save(reglaCuotaReal(CategoriaSocio.ACTIVO, "Regla A", "10000.00", 10));
            reglaCuotaRepositoryReal.save(reglaCuotaReal(CategoriaSocio.ACTIVO, "Regla B", "12000.00", 15));
            reglaCuotaRepositoryReal.save(reglaCuotaReal(CategoriaSocio.ADHERENTE, "Regla C", "5000.00", 10));
            socioRepositoryReal.save(socioActivoReal("SOC-000001", CategoriaSocio.ADHERENTE, "socio@example.com"));

            GeneracionCuotasResponse response = cuotaServiceReal.generarCuotas("2026-07", null, null);

            assertThat(response.cantidadCuotasGeneradas()).isEqualTo(1);
            assertThat(response.cantidadSociosOmitidos()).isEqualTo(0);
            verify(reglaCuotaRepositoryReal, times(1)).findAll();
            verify(reglaCuotaRepositoryReal, never()).findByCategoriaAplicable(any());
        }

        /**
         * Foto record-level con semilla fija: socio1 (ACTIVO, con email) y socio5
         * (ACTIVO, con email) reciben cuota + correo; socio2 (ADHERENTE, sin regla)
         * se omite; socio3 (ACTIVO) ya tenía cuota del período, no se duplica;
         * socio4 (ACTIVO, sin email) recibe cuota pero ningún correo. Este mismo
         * test corre sin cambios antes (fase 1) y después (fase 5) del refactor
         * D-4/D-5: si sigue pasando, el comportamiento se preservó byte a byte
         * (campos volátiles enmascarados vía rango de tiempo, no valor exacto).
         */
        @Test
        void generarCuotas_conSemillaFija_generaLasCuotasEsperadasYRespetaOrdenDeEnvioDeCorreos() {
            reglaCuotaRepositoryReal.save(reglaCuotaReal(CategoriaSocio.ACTIVO, "Cuota de socio activo", "15000.00", 10));

            Socio socio1 = socioRepositoryReal.save(socioActivoReal("SOC-000001", CategoriaSocio.ACTIVO, "socio1@example.com"));
            Socio socio2 = socioRepositoryReal.save(socioActivoReal("SOC-000002", CategoriaSocio.ADHERENTE, "socio2@example.com"));
            Socio socio3 = socioRepositoryReal.save(socioActivoReal("SOC-000003", CategoriaSocio.ACTIVO, "socio3@example.com"));
            Socio socio4 = socioRepositoryReal.save(socioActivoReal("SOC-000004", CategoriaSocio.ACTIVO, null));
            Socio socio5 = socioRepositoryReal.save(socioActivoReal("SOC-000005", CategoriaSocio.ACTIVO, "socio5@example.com"));

            Cuota cuotaPreexistenteSocio3 = cuotaRepositoryReal.save(Cuota.builder()
                    .socioId(socio3.getId())
                    .socioNumeroSocio(socio3.getNumeroSocio())
                    .socioNombre(socio3.nombreParaMostrar())
                    .tipoCuotaNombre("Cuota de socio activo")
                    .categoria(CategoriaSocio.ACTIVO)
                    .periodo("2026-07")
                    .importe(new BigDecimal("15000.00"))
                    .fechaVencimiento(LocalDate.of(2026, 7, 10))
                    .estado(EstadoCuota.PENDIENTE)
                    .fechaGeneracion(Instant.now())
                    .fechaActualizacion(Instant.now())
                    .build());

            Instant antes = Instant.now();
            GeneracionCuotasResponse response = cuotaServiceReal.generarCuotas("2026-07", null, null);
            Instant despues = Instant.now();

            assertThat(response.cantidadSociosActivos()).isEqualTo(5);
            assertThat(response.cantidadCuotasGeneradas()).isEqualTo(3);
            assertThat(response.cantidadSociosOmitidos()).isEqualTo(1);
            assertThat(response.origen()).isEqualTo(OrigenEjecucionCuotas.AUTOMATICA);

            // D-4/D-5: un único findAll() de reglas y un único findByPeriodo() de cuotas
            // por corrida, nunca un query por-socio dentro del loop.
            verify(reglaCuotaRepositoryReal, times(1)).findAll();
            verify(reglaCuotaRepositoryReal, never()).findByCategoriaAplicable(any());
            verify(cuotaRepositoryReal, times(1)).findByPeriodo("2026-07");
            verify(cuotaRepositoryReal, never()).existsBySocioIdAndPeriodo(anyString(), anyString());

            Cuota cuotaSocio1 = cuotaRepositoryReal.findBySocioIdAndPeriodo(socio1.getId(), "2026-07").orElseThrow();
            assertThat(cuotaSocio1.getSocioNumeroSocio()).isEqualTo("SOC-000001");
            assertThat(cuotaSocio1.getTipoCuotaNombre()).isEqualTo("Cuota de socio activo");
            assertThat(cuotaSocio1.getCategoria()).isEqualTo(CategoriaSocio.ACTIVO);
            assertThat(cuotaSocio1.getImporte()).isEqualByComparingTo("15000.00");
            assertThat(cuotaSocio1.getFechaVencimiento()).isEqualTo(LocalDate.of(2026, 7, 10));
            assertThat(cuotaSocio1.getEstado()).isEqualTo(EstadoCuota.PENDIENTE);
            assertThat(cuotaSocio1.getFechaGeneracion()).isBetween(antes, despues);
            assertThat(cuotaSocio1.getFechaActualizacion()).isBetween(antes, despues);

            assertThat(cuotaRepositoryReal.findBySocioIdAndPeriodo(socio2.getId(), "2026-07")).isEmpty();

            List<Cuota> cuotasSocio3 = cuotaRepositoryReal.findBySocioId(socio3.getId());
            assertThat(cuotasSocio3).hasSize(1);
            assertThat(cuotasSocio3.get(0).getId()).isEqualTo(cuotaPreexistenteSocio3.getId());

            Cuota cuotaSocio4 = cuotaRepositoryReal.findBySocioIdAndPeriodo(socio4.getId(), "2026-07").orElseThrow();
            assertThat(cuotaSocio4.getImporte()).isEqualByComparingTo("15000.00");
            assertThat(cuotaSocio4.getEstado()).isEqualTo(EstadoCuota.PENDIENTE);

            Cuota cuotaSocio5 = cuotaRepositoryReal.findBySocioIdAndPeriodo(socio5.getId(), "2026-07").orElseThrow();
            assertThat(cuotaSocio5.getImporte()).isEqualByComparingTo("15000.00");

            // Solo socio1 y socio5 tienen email: socio4 (sin email) no dispara envío, y
            // el orden de los dos envíos reales sigue el orden de sociosActivos.
            InOrder orden = inOrder(emailServiceReal);
            orden.verify(emailServiceReal).enviarCorreoCuotaGenerada(
                    eq("socio1@example.com"), anyString(), eq("2026-07"), eq(new BigDecimal("15000.00")), eq(LocalDate.of(2026, 7, 10)));
            orden.verify(emailServiceReal).enviarCorreoCuotaGenerada(
                    eq("socio5@example.com"), anyString(), eq("2026-07"), eq(new BigDecimal("15000.00")), eq(LocalDate.of(2026, 7, 10)));
            verify(emailServiceReal, times(2)).enviarCorreoCuotaGenerada(anyString(), anyString(), anyString(), any(), any());
        }
    }
}
