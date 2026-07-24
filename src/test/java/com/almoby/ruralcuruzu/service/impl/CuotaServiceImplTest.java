package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.almoby.ruralcuruzu.domain.DatosPersonaFisica;
import com.almoby.ruralcuruzu.domain.EjecucionGeneracionCuotas;
import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.domain.TipoCuota;
import com.almoby.ruralcuruzu.dto.request.AnularCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.InformarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RegistrarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RevisarPagoInformadoRequest;
import com.almoby.ruralcuruzu.dto.response.CuotaResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoCuentaSocioResponse;
import com.almoby.ruralcuruzu.dto.response.GeneracionCuotasResponse;
import com.almoby.ruralcuruzu.dto.response.InformarPagoResponse;
import com.almoby.ruralcuruzu.dto.response.RegistrarPagoResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoCuota;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.EstadoTipoCuota;
import com.almoby.ruralcuruzu.enums.MedioPago;
import com.almoby.ruralcuruzu.enums.OrigenEjecucionCuotas;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.exception.CuotaEstadoInvalidoException;
import com.almoby.ruralcuruzu.exception.CuotaNoEncontradaException;
import com.almoby.ruralcuruzu.exception.SocioNoEncontradoException;
import com.almoby.ruralcuruzu.repository.CuotaRepository;
import com.almoby.ruralcuruzu.repository.EjecucionGeneracionCuotasRepository;
import com.almoby.ruralcuruzu.repository.SocioRepository;
import com.almoby.ruralcuruzu.repository.TipoCuotaRepository;
import com.almoby.ruralcuruzu.service.EmailService;

@ExtendWith(MockitoExtension.class)
class CuotaServiceImplTest {

    @Mock
    private CuotaRepository cuotaRepository;
    @Mock
    private TipoCuotaRepository tipoCuotaRepository;
    @Mock
    private EjecucionGeneracionCuotasRepository ejecucionRepository;
    @Mock
    private SocioRepository socioRepository;
    @Mock
    private EmailService emailService;

    private CuotaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CuotaServiceImpl(cuotaRepository, tipoCuotaRepository, ejecucionRepository, socioRepository, emailService);
    }

    private Socio socioActivo(String id, String numeroSocio, CategoriaSocio categoria) {
        DatosPersonaFisica datos = new DatosPersonaFisica(
                "Juan", "Lopez", "12345678", null, null, null, null, null, "juan@example.com", null, null, null);
        return Socio.builder()
                .id(id)
                .numeroSocio(numeroSocio)
                .categoria(categoria)
                .tipoPersona(TipoPersona.FISICA)
                .datosPersonaFisica(datos)
                .estado(EstadoSocio.ACTIVO)
                .build();
    }

    private TipoCuota tipoCuotaVigente(CategoriaSocio categoria) {
        return TipoCuota.builder()
                .id("tipo-1")
                .nombre("Cuota de socio activo")
                .categoriaAplicable(categoria)
                .importe(new BigDecimal("15000.00"))
                .fechaVigencia(LocalDate.of(2020, 1, 1))
                .diaVencimiento(10)
                .estado(EstadoTipoCuota.ACTIVO)
                .build();
    }

    private Cuota cuotaPendiente(String id, String socioId) {
        return Cuota.builder()
                .id(id)
                .socioId(socioId)
                .socioNumeroSocio("SOC-000001")
                .socioNombre("Lopez, Juan")
                .tipoCuotaId("tipo-1")
                .tipoCuotaNombre("Cuota de socio activo")
                .categoria(CategoriaSocio.ACTIVO)
                .periodo("2026-07")
                .importe(new BigDecimal("15000.00"))
                .fechaVencimiento(LocalDate.of(2026, 7, 10))
                .estado(EstadoCuota.PENDIENTE)
                .build();
    }

    // ---------- generarCuotas ----------

    @Test
    void generarCuotas_conSocioActivoYTipoVigente_generaCuotaPendiente() {
        Socio socio = socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO);
        when(socioRepository.findByEstado(EstadoSocio.ACTIVO)).thenReturn(List.of(socio));
        when(cuotaRepository.existsBySocioIdAndPeriodo(eq("socio-1"), anyString())).thenReturn(false);
        when(tipoCuotaRepository.findFirstByCategoriaAplicableAndEstadoAndFechaVigenciaLessThanEqualOrderByFechaVigenciaDesc(
                eq(CategoriaSocio.ACTIVO), eq(EstadoTipoCuota.ACTIVO), any(LocalDate.class)))
                .thenReturn(Optional.of(tipoCuotaVigente(CategoriaSocio.ACTIVO)));
        when(ejecucionRepository.save(any(EjecucionGeneracionCuotas.class))).thenAnswer(inv -> inv.getArgument(0));

        GeneracionCuotasResponse response = service.generarCuotas("2026-07", null, null);

        assertThat(response.cantidadCuotasGeneradas()).isEqualTo(1);
        assertThat(response.cantidadSociosOmitidos()).isEqualTo(0);
        assertThat(response.origen()).isEqualTo(OrigenEjecucionCuotas.AUTOMATICA);
        verify(cuotaRepository).save(any(Cuota.class));
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
    void generarCuotas_conSocioSinTipoVigente_loOmiteYNoRompeLaCorrida() {
        Socio socio = socioActivo("socio-1", "SOC-000001", CategoriaSocio.ADHERENTE);
        when(socioRepository.findByEstado(EstadoSocio.ACTIVO)).thenReturn(List.of(socio));
        when(cuotaRepository.existsBySocioIdAndPeriodo(eq("socio-1"), anyString())).thenReturn(false);
        when(tipoCuotaRepository.findFirstByCategoriaAplicableAndEstadoAndFechaVigenciaLessThanEqualOrderByFechaVigenciaDesc(
                eq(CategoriaSocio.ADHERENTE), eq(EstadoTipoCuota.ACTIVO), any(LocalDate.class)))
                .thenReturn(Optional.empty());
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
        verify(tipoCuotaRepository, never()).findFirstByCategoriaAplicableAndEstadoAndFechaVigenciaLessThanEqualOrderByFechaVigenciaDesc(
                any(), any(), any());
        verify(cuotaRepository, never()).save(any());
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
        Cuota otroSocio = cuotaPendiente("cuota-2", "socio-2");
        when(cuotaRepository.findBySocioId("socio-1")).thenReturn(List.of(coincide));

        List<?> resultado = service.listarCuotas(EstadoCuota.PENDIENTE, "socio-1", "2026-07");

        assertThat(resultado).hasSize(1);
    }

    // ---------- registrarPago ----------

    @Test
    void registrarPago_conCuotaPendiente_marcaPagadaYMandaCorreo() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        Socio socio = socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO);
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socio));

        RegistrarPagoCuotaRequest request = new RegistrarPagoCuotaRequest(
                List.of("cuota-1"), LocalDate.of(2026, 7, 5), new BigDecimal("15000.00"),
                MedioPago.TRANSFERENCIA, "COMP-1", "ok");

        RegistrarPagoResponse response = service.registrarPago(request, "admin-1", "Admin Uno");

        assertThat(response.mensaje()).isEqualTo("Pago registrado con éxito");
        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.PAGADA);
        assertThat(cuota.getDatosPago().getRegistradoPorAdminNombre()).isEqualTo("Admin Uno");
        verify(cuotaRepository).save(cuota);
        verify(emailService).enviarCorreoPagoRegistrado(eq("juan@example.com"), anyString(), eq("2026-07"), any());
    }

    @Test
    void registrarPago_conCuotaYaPagada_lanzaExcepcion() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.PAGADA);
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        RegistrarPagoCuotaRequest request = new RegistrarPagoCuotaRequest(
                List.of("cuota-1"), LocalDate.of(2026, 7, 5), new BigDecimal("15000.00"),
                MedioPago.EFECTIVO, null, null);

        assertThatThrownBy(() -> service.registrarPago(request, "admin-1", "Admin Uno"))
                .isInstanceOf(CuotaEstadoInvalidoException.class);
    }

    // ---------- informarPago ----------

    @Test
    void informarPago_conCuotaPropiaPendiente_pasaAEnRevision() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        InformarPagoCuotaRequest request = new InformarPagoCuotaRequest(
                LocalDate.of(2026, 7, 5), new BigDecimal("15000.00"), MedioPago.TRANSFERENCIA, "COMP-1", null);

        InformarPagoResponse response = service.informarPago("cuota-1", request, "socio-1");

        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.EN_REVISION);
        assertThat(cuota.getDatosPago().isInformadoPorSocio()).isTrue();
        assertThat(response.cuota().estado()).isEqualTo(EstadoCuota.EN_REVISION);
    }

    @Test
    void informarPago_deCuotaDeOtroSocio_lanzaCuotaNoEncontrada() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        InformarPagoCuotaRequest request = new InformarPagoCuotaRequest(
                LocalDate.of(2026, 7, 5), new BigDecimal("15000.00"), MedioPago.TRANSFERENCIA, null, null);

        assertThatThrownBy(() -> service.informarPago("cuota-1", request, "socio-ajeno"))
                .isInstanceOf(CuotaNoEncontradaException.class);
    }

    @Test
    void informarPago_conCuotaEnEstadoInvalido_lanzaExcepcion() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.PAGADA);
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        InformarPagoCuotaRequest request = new InformarPagoCuotaRequest(
                LocalDate.of(2026, 7, 5), new BigDecimal("15000.00"), MedioPago.TRANSFERENCIA, null, null);

        assertThatThrownBy(() -> service.informarPago("cuota-1", request, "socio-1"))
                .isInstanceOf(CuotaEstadoInvalidoException.class);
    }

    // ---------- revisarPagoInformado ----------

    @Test
    void revisarPagoInformado_aprobar_marcaPagada() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.EN_REVISION);
        cuota.setDatosPago(DatosPago.builder().importe(new BigDecimal("15000.00")).informadoPorSocio(true).build());
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO)));

        CuotaResponse response = service.revisarPagoInformado(
                "cuota-1", new RevisarPagoInformadoRequest(true, null), "admin-1", "Admin Uno");

        assertThat(response.estado()).isEqualTo(EstadoCuota.PAGADA);
        assertThat(cuota.getDatosPago().getRegistradoPorAdminNombre()).isEqualTo("Admin Uno");
        verify(emailService).enviarCorreoPagoRegistrado(anyString(), anyString(), anyString(), any());
    }

    @Test
    void revisarPagoInformado_rechazarSinMotivo_lanzaExcepcion() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.EN_REVISION);
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));

        assertThatThrownBy(() -> service.revisarPagoInformado(
                "cuota-1", new RevisarPagoInformadoRequest(false, null), "admin-1", "Admin Uno"))
                .isInstanceOf(CuotaEstadoInvalidoException.class);
    }

    @Test
    void revisarPagoInformado_rechazarConMotivo_marcaRechazada() {
        Cuota cuota = cuotaPendiente("cuota-1", "socio-1");
        cuota.setEstado(EstadoCuota.EN_REVISION);
        when(cuotaRepository.findById("cuota-1")).thenReturn(Optional.of(cuota));
        when(socioRepository.findById("socio-1")).thenReturn(Optional.of(socioActivo("socio-1", "SOC-000001", CategoriaSocio.ACTIVO)));

        CuotaResponse response = service.revisarPagoInformado(
                "cuota-1", new RevisarPagoInformadoRequest(false, "Comprobante ilegible"), "admin-1", "Admin Uno");

        assertThat(response.estado()).isEqualTo(EstadoCuota.RECHAZADA);
        assertThat(response.motivoRechazo()).isEqualTo("Comprobante ilegible");
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
}
