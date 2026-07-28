package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.almoby.ruralcuruzu.domain.Cuota;
import com.almoby.ruralcuruzu.domain.DatosPersonaFisica;
import com.almoby.ruralcuruzu.domain.EjecucionGeneracionCuotas;
import com.almoby.ruralcuruzu.domain.Pago;
import com.almoby.ruralcuruzu.domain.ReglaCuota;
import com.almoby.ruralcuruzu.domain.Socio;
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
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.exception.ArchivoInvalidoException;
import com.almoby.ruralcuruzu.exception.CuotaEstadoInvalidoException;
import com.almoby.ruralcuruzu.exception.CuotaNoEncontradaException;
import com.almoby.ruralcuruzu.exception.SocioNoEncontradoException;
import com.almoby.ruralcuruzu.repository.CuotaRepository;
import com.almoby.ruralcuruzu.repository.EjecucionGeneracionCuotasRepository;
import com.almoby.ruralcuruzu.repository.PagoRepository;
import com.almoby.ruralcuruzu.repository.ReglaCuotaRepository;
import com.almoby.ruralcuruzu.repository.SocioRepository;
import com.almoby.ruralcuruzu.service.AlmacenamientoComprobantesService;
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

    private CuotaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CuotaServiceImpl(cuotaRepository, pagoRepository, ejecucionRepository, socioRepository,
                emailService, reglaCuotaRepository, almacenamientoComprobantesService, mercadoPagoService);

        // Default: ningún pago vigente salvo que un test lo indique explícitamente
        // (evita repetir este stub en cada test que no necesita un Pago).
        lenient().when(pagoRepository.findByCuotaIdIn(anyList())).thenReturn(List.of());
        lenient().when(pagoRepository.findByCuotaIdAndEstado(anyString(), any(EstadoPago.class)))
                .thenReturn(Optional.empty());
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
        DatosPersonaFisica datos = new DatosPersonaFisica(
                "Lopez, Juan", "12345678", null, null, null, null, null, "juan@example.com", null, null);
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
        when(cuotaRepository.existsBySocioIdAndPeriodo(eq("socio-1"), anyString())).thenReturn(false);
        when(reglaCuotaRepository.findByCategoriaAplicable(CategoriaSocio.ACTIVO))
                .thenReturn(Optional.of(reglaCuota(CategoriaSocio.ACTIVO, "Cuota de socio activo", "15000.00", 10)));
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
        when(cuotaRepository.existsBySocioIdAndPeriodo(eq("socio-1"), anyString())).thenReturn(false);
        when(reglaCuotaRepository.findByCategoriaAplicable(CategoriaSocio.ADHERENTE)).thenReturn(Optional.empty());
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
        when(cuotaRepository.existsBySocioIdAndPeriodo("socio-1", "2026-07")).thenReturn(true);
        when(ejecucionRepository.save(any(EjecucionGeneracionCuotas.class))).thenAnswer(inv -> inv.getArgument(0));

        GeneracionCuotasResponse response = service.generarCuotas("2026-07", null, null);

        assertThat(response.cantidadCuotasGeneradas()).isEqualTo(0);
        verify(cuotaRepository, never()).save(any());
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

    // ---------- registrarPago ----------

    @Test
    void registrarPago_conCuotaPendiente_marcaPagadaYMandaCorreo() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        Socio socio = socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO);
        when(cuotaRepository.findBySocioIdAndPeriodo("socio-1", "2026-07")).thenReturn(Optional.of(cuota));
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio));

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
        when(pagoRepository.findByCuotaIdAndEstado("cuota-1", EstadoPago.EN_REVISION)).thenReturn(Optional.of(pago));
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
        when(pagoRepository.findByCuotaIdAndEstado("cuota-1", EstadoPago.EN_REVISION)).thenReturn(Optional.of(pago));

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
        when(pagoRepository.findByCuotaIdAndEstado("cuota-1", EstadoPago.EN_REVISION)).thenReturn(Optional.of(pago));
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
        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.EN_REVISION);
        verify(pagoRepository, times(2)).save(any(Pago.class));
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
        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.EN_REVISION);
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
    void procesarNotificacionMercadoPago_pendiente_noCambiaEstados() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.EN_REVISION);
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
    }
}
