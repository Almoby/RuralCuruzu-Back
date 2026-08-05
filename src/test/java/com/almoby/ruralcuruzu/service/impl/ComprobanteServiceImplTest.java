package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.almoby.ruralcuruzu.domain.Comprobante;
import com.almoby.ruralcuruzu.domain.Pago;
import com.almoby.ruralcuruzu.dto.response.PagoResponse;
import com.almoby.ruralcuruzu.enums.EstadoPago;
import com.almoby.ruralcuruzu.enums.MedioPago;
import com.almoby.ruralcuruzu.enums.OrigenComprobante;
import com.almoby.ruralcuruzu.repository.ComprobanteRepository;
import com.almoby.ruralcuruzu.service.AlmacenamientoComprobantesService;
import com.almoby.ruralcuruzu.service.ComprobantePagoPdfService;

@ExtendWith(MockitoExtension.class)
class ComprobanteServiceImplTest {

    @Mock
    private ComprobanteRepository comprobanteRepository;
    @Mock
    private AlmacenamientoComprobantesService almacenamientoComprobantesService;
    @Mock
    private ComprobantePagoPdfService comprobantePagoPdfService;

    private ComprobanteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ComprobanteServiceImpl(
                comprobanteRepository, almacenamientoComprobantesService, comprobantePagoPdfService);
    }

    private PagoResponse pagoResponse(String id, String comprobanteRuta, EstadoPago estado, MedioPago medioPago) {
        return pagoResponse(id, comprobanteRuta, estado, medioPago, false);
    }

    private PagoResponse pagoResponse(String id, String comprobanteRuta, EstadoPago estado, MedioPago medioPago,
                                       boolean informadoPorSocio) {
        return new PagoResponse(id, "cuota-1", "socio-1", "S-100", "Juan Pérez", "2026-07",
                BigDecimal.valueOf(3500), medioPago, estado, Instant.now(), comprobanteRuta,
                null, informadoPorSocio, "Admin Uno", null, Instant.now());
    }

    @Test
    void registrarSubidoPorSocio_guardaConDatosDelArchivoOriginal() {
        Pago pago = Pago.builder().id("pago-1").cuotaId("cuota-1").socioId("socio-1").build();
        MultipartFile archivo = new MockMultipartFile(
                "comprobante", "transferencia.pdf", "application/pdf", "contenido".getBytes());

        Comprobante resultado = service.registrarSubidoPorSocio(pago, "pago-1/uuid_transferencia.pdf", archivo);

        ArgumentCaptor<Comprobante> captor = ArgumentCaptor.forClass(Comprobante.class);
        verify(comprobanteRepository).save(captor.capture());
        Comprobante guardado = captor.getValue();

        assertThat(guardado.getPagoId()).isEqualTo("pago-1");
        assertThat(guardado.getCuotaId()).isEqualTo("cuota-1");
        assertThat(guardado.getSocioId()).isEqualTo("socio-1");
        assertThat(guardado.getOrigen()).isEqualTo(OrigenComprobante.SUBIDO_POR_SOCIO);
        assertThat(guardado.getNombreArchivo()).isEqualTo("transferencia.pdf");
        assertThat(guardado.getRuta()).isEqualTo("pago-1/uuid_transferencia.pdf");
        assertThat(guardado.getContentType()).isEqualTo("application/pdf");
        assertThat(guardado.getTamanioBytes()).isEqualTo("contenido".getBytes().length);
        assertThat(resultado).isSameAs(guardado);
    }

    @Test
    void obtenerOGenerarParaPago_conComprobanteYaRegistrado_loDevuelveTalCual() {
        Comprobante existente = Comprobante.builder().id("c-1").pagoId("pago-1").build();
        when(comprobanteRepository.findByPagoId("pago-1")).thenReturn(Optional.of(existente));

        Optional<Comprobante> resultado = service.obtenerOGenerarParaPago(
                pagoResponse("pago-1", null, EstadoPago.APROBADO, MedioPago.LINK_DE_PAGO), "socio-1");

        assertThat(resultado).contains(existente);
        verify(comprobantePagoPdfService, never()).generarConstancia(any());
        verify(almacenamientoComprobantesService, never()).guardarBytes(anyString(), anyString(), any());
    }

    @Test
    void obtenerOGenerarParaPago_conDatoLegacyComprobanteRuta_loMigraYLoDevuelve() {
        when(comprobanteRepository.findByPagoId("pago-1")).thenReturn(Optional.empty());

        Optional<Comprobante> resultado = service.obtenerOGenerarParaPago(
                pagoResponse("pago-1", "pago-1/uuid_viejo.pdf", EstadoPago.APROBADO, MedioPago.TRANSFERENCIA, true),
                "socio-1");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getOrigen()).isEqualTo(OrigenComprobante.SUBIDO_POR_SOCIO);
        assertThat(resultado.get().getRuta()).isEqualTo("pago-1/uuid_viejo.pdf");
        assertThat(resultado.get().getNombreArchivo()).isEqualTo("uuid_viejo.pdf");
        verify(comprobanteRepository).save(any(Comprobante.class));
        verify(comprobantePagoPdfService, never()).generarConstancia(any());
    }

    @Test
    void obtenerOGenerarParaPago_conNotaDeTextoDeAdminYPagoAprobado_generaConstanciaEnVezDeMigrarLaNota() {
        // Bug real que este test evita que vuelva a pasar desapercibido: cuando el
        // admin registra un pago (registrarPago), Pago.comprobanteRuta no es un
        // archivo real, es una nota de texto libre que tipeó (ej. "Recibo #123",
        // ver RegistrarPagoCuotaRequest.comprobante()). Si se tratara como si fuera
        // una ruta real (como en el caso legacy de una transferencia), el
        // Comprobante quedaría apuntando a un "archivo" que no existe en disco.
        when(comprobanteRepository.findByPagoId("pago-9")).thenReturn(Optional.empty());
        PagoResponse pago = pagoResponse(
                "pago-9", "Recibo #123", EstadoPago.APROBADO, MedioPago.VENTANILLA, false);
        when(comprobantePagoPdfService.generarConstancia(pago)).thenReturn(new byte[] {9});
        when(almacenamientoComprobantesService.guardarBytes(eq("pago-9"), anyString(), any()))
                .thenReturn("pago-9/uuid_constancia-pago-pago-9.pdf");

        Optional<Comprobante> resultado = service.obtenerOGenerarParaPago(pago, "socio-1");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getOrigen()).isEqualTo(OrigenComprobante.GENERADO_POR_SISTEMA);
        assertThat(resultado.get().getRuta()).isEqualTo("pago-9/uuid_constancia-pago-pago-9.pdf");
        verify(comprobantePagoPdfService).generarConstancia(pago);
    }

    @Test
    void obtenerOGenerarParaPago_sinNadaYPagoAprobado_generaLaConstanciaYLaPersiste() {
        when(comprobanteRepository.findByPagoId("pago-2")).thenReturn(Optional.empty());
        PagoResponse pago = pagoResponse("pago-2", null, EstadoPago.APROBADO, MedioPago.VENTANILLA);
        when(comprobantePagoPdfService.generarConstancia(pago)).thenReturn(new byte[] {1, 2, 3});
        when(almacenamientoComprobantesService.guardarBytes(eq("pago-2"), anyString(), any()))
                .thenReturn("pago-2/uuid_constancia-pago-pago-2.pdf");

        Optional<Comprobante> resultado = service.obtenerOGenerarParaPago(pago, "socio-1");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getOrigen()).isEqualTo(OrigenComprobante.GENERADO_POR_SISTEMA);
        assertThat(resultado.get().getContentType()).isEqualTo("application/pdf");
        assertThat(resultado.get().getRuta()).isEqualTo("pago-2/uuid_constancia-pago-pago-2.pdf");
        assertThat(resultado.get().getTamanioBytes()).isEqualTo(3);
        verify(comprobanteRepository, times(1)).save(any(Comprobante.class));
    }

    @Test
    void obtenerOGenerarParaPago_sinNadaYPagoNoAprobado_devuelveVacioSinGenerarNada() {
        when(comprobanteRepository.findByPagoId("pago-3")).thenReturn(Optional.empty());
        PagoResponse pago = pagoResponse("pago-3", null, EstadoPago.EN_REVISION, MedioPago.LINK_DE_PAGO);

        Optional<Comprobante> resultado = service.obtenerOGenerarParaPago(pago, "socio-1");

        assertThat(resultado).isEmpty();
        verify(comprobantePagoPdfService, never()).generarConstancia(any());
        verify(comprobanteRepository, never()).save(any());
    }
}
