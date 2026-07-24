package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.almoby.ruralcuruzu.domain.Comercio;
import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.dto.request.AltaComercioRequest;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoComercioRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoComercioResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioCreadoResponse;
import com.almoby.ruralcuruzu.enums.EstadoComercio;
import com.almoby.ruralcuruzu.enums.EstadoUsuario;
import com.almoby.ruralcuruzu.enums.Rol;
import com.almoby.ruralcuruzu.exception.ComercioNoEncontradoException;
import com.almoby.ruralcuruzu.exception.CorreoYaRegistradoException;
import com.almoby.ruralcuruzu.exception.CuitYaRegistradoException;
import com.almoby.ruralcuruzu.repository.ComercioRepository;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.service.CuentaAccesoService;
import com.almoby.ruralcuruzu.service.EmailService;

@ExtendWith(MockitoExtension.class)
class ComercioServiceImplTest {

    @Mock
    private ComercioRepository comercioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CuentaAccesoService cuentaAccesoService;
    @Mock
    private EmailService emailService;

    private ComercioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ComercioServiceImpl(comercioRepository, usuarioRepository, cuentaAccesoService, emailService);
    }

    private AltaComercioRequest requestValido() {
        return new AltaComercioRequest(
                "Almacén Don José", "Don José S.R.L.", "30-71234567-9", "Almacén y despensa",
                "+54 9 3777123456", "contacto@donjose.com", "Ruta 123 km 4", null, null, null);
    }

    private CuentaAccesoService.CuentaTemporalCreada cuentaFalsa(String email) {
        Usuario usuario = Usuario.builder()
                .id("usuario-generado-1")
                .email(email)
                .passwordHash("hash-encriptado")
                .rol(Rol.COMERCIO)
                .estado(EstadoUsuario.ACTIVO)
                .requiereCambioPassword(true)
                .build();
        return new CuentaAccesoService.CuentaTemporalCreada(usuario, "PasswordTemp1");
    }

    private void doAnswerAsignarIdComercio() {
        when(comercioRepository.save(any(Comercio.class))).thenAnswer(invocation -> {
            Comercio comercio = invocation.getArgument(0);
            if (comercio.getId() == null) {
                comercio.setId("comercio-generado-1");
            }
            return comercio;
        });
    }

    @Test
    void crearComercio_conDatosValidos_creaComercioActivoConUsuarioYMandaCorreo() {
        when(comercioRepository.existsByCuit(anyString())).thenReturn(false);
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(comercioRepository.existsByCorreoElectronicoIgnoreCase(anyString())).thenReturn(false);
        when(cuentaAccesoService.crearCuentaConPasswordTemporal(anyString(), anyString(), eq(Rol.COMERCIO), anyString()))
                .thenReturn(cuentaFalsa("contacto@donjose.com"));
        doAnswerAsignarIdComercio();

        ComercioCreadoResponse response = service.crearComercio(requestValido(), "admin-1", "Admin Uno");

        assertThat(response.mensaje()).isEqualTo("Comercio dado de alta con éxito");
        assertThat(response.comercio().nombreComercial()).isEqualTo("Almacén Don José");
        assertThat(response.comercio().estado()).isEqualTo(EstadoComercio.ACTIVO);
        verify(comercioRepository, times(2)).save(any(Comercio.class));
        verify(emailService).enviarCorreoCredencialesComercio(
                eq("contacto@donjose.com"), eq("Almacén Don José"), eq("PasswordTemp1"));
    }

    @Test
    void crearComercio_conEstadoEspecificado_respetaEseEstadoInicial() {
        AltaComercioRequest request = new AltaComercioRequest(
                "Almacén Don José", "Don José S.R.L.", "30-71234567-9", "Almacén y despensa",
                "+54 9 3777123456", "contacto@donjose.com", "Ruta 123 km 4", null, null, EstadoComercio.INACTIVO);

        when(comercioRepository.existsByCuit(anyString())).thenReturn(false);
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(comercioRepository.existsByCorreoElectronicoIgnoreCase(anyString())).thenReturn(false);
        when(cuentaAccesoService.crearCuentaConPasswordTemporal(anyString(), anyString(), eq(Rol.COMERCIO), anyString()))
                .thenReturn(cuentaFalsa("contacto@donjose.com"));
        doAnswerAsignarIdComercio();

        ComercioCreadoResponse response = service.crearComercio(request, "admin-1", "Admin Uno");

        assertThat(response.comercio().estado()).isEqualTo(EstadoComercio.INACTIVO);
    }

    @Test
    void crearComercio_conCuitYaRegistrado_lanzaExcepcion() {
        when(comercioRepository.existsByCuit(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.crearComercio(requestValido(), "admin-1", "Admin Uno"))
                .isInstanceOf(CuitYaRegistradoException.class);

        verify(comercioRepository, never()).save(any());
    }

    @Test
    void crearComercio_conCorreoYaRegistradoComoUsuario_lanzaExcepcion() {
        when(comercioRepository.existsByCuit(anyString())).thenReturn(false);
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.crearComercio(requestValido(), "admin-1", "Admin Uno"))
                .isInstanceOf(CorreoYaRegistradoException.class);

        verify(comercioRepository, never()).save(any());
    }

    @Test
    void crearComercio_conCorreoYaRegistradoComoOtroComercio_lanzaExcepcion() {
        when(comercioRepository.existsByCuit(anyString())).thenReturn(false);
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(comercioRepository.existsByCorreoElectronicoIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.crearComercio(requestValido(), "admin-1", "Admin Uno"))
                .isInstanceOf(CorreoYaRegistradoException.class);

        verify(comercioRepository, never()).save(any());
    }

    @Test
    void cambiarEstadoComercio_actualizaYDevuelveMensaje() {
        Comercio comercio = Comercio.builder().id("comercio-1").estado(EstadoComercio.ACTIVO).build();
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio));

        CambiarEstadoComercioResponse response = service.cambiarEstadoComercio(
                "comercio-1", new CambiarEstadoComercioRequest(EstadoComercio.SUSPENDIDO));

        assertThat(response.estado()).isEqualTo(EstadoComercio.SUSPENDIDO);
        assertThat(response.mensaje()).isEqualTo("Comercio suspendido correctamente");
        assertThat(comercio.getEstado()).isEqualTo(EstadoComercio.SUSPENDIDO);
        verify(comercioRepository).save(comercio);
    }

    @Test
    void cambiarEstadoComercio_inexistente_lanzaExcepcion() {
        when(comercioRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstadoComercio(
                "no-existe", new CambiarEstadoComercioRequest(EstadoComercio.SUSPENDIDO)))
                .isInstanceOf(ComercioNoEncontradoException.class);
    }

    @Test
    void obtenerComercioPorId_inexistente_lanzaExcepcion() {
        when(comercioRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerComercioPorId("no-existe"))
                .isInstanceOf(ComercioNoEncontradoException.class);
    }

    @Test
    void listarComercios_sinFiltroDeEstado_usaFindAll() {
        Comercio comercio = Comercio.builder().id("comercio-1").estado(EstadoComercio.ACTIVO).build();
        when(comercioRepository.findAll()).thenReturn(java.util.List.of(comercio));

        java.util.List<?> resultado = service.listarComercios(null);

        assertThat(resultado).hasSize(1);
        verify(comercioRepository, never()).findByEstado(any());
    }

    @Test
    void listarComercios_conFiltroDeEstado_usaFindByEstado() {
        Comercio comercio = Comercio.builder().id("comercio-1").estado(EstadoComercio.ACTIVO).build();
        when(comercioRepository.findByEstado(EstadoComercio.ACTIVO)).thenReturn(java.util.List.of(comercio));

        java.util.List<?> resultado = service.listarComercios(EstadoComercio.ACTIVO);

        assertThat(resultado).hasSize(1);
        verify(comercioRepository, never()).findAll();
    }
}
