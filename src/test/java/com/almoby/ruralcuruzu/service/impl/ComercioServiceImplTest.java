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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.domain.Comercio;
import com.almoby.ruralcuruzu.domain.ComercioEliminado;
import com.almoby.ruralcuruzu.domain.HistorialBeneficio;
import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.dto.request.ActualizarComercioParcialRequest;
import com.almoby.ruralcuruzu.dto.request.AltaComercioRequest;
import com.almoby.ruralcuruzu.dto.request.CambiarEstadoComercioRequest;
import com.almoby.ruralcuruzu.dto.request.EliminarComercioRequest;
import com.almoby.ruralcuruzu.dto.response.CambiarEstadoComercioResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioActualizadoResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioCreadoResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioEliminadoResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioResponse;
import com.almoby.ruralcuruzu.dto.response.ComercioResumenResponse;
import com.almoby.ruralcuruzu.dto.response.EliminarComercioResponse;
import com.almoby.ruralcuruzu.enums.EstadoBeneficio;
import com.almoby.ruralcuruzu.enums.EstadoComercio;
import com.almoby.ruralcuruzu.enums.EstadoUsoBeneficio;
import com.almoby.ruralcuruzu.enums.EstadoUsuario;
import com.almoby.ruralcuruzu.enums.Rol;
import com.almoby.ruralcuruzu.exception.ComercioNoEncontradoException;
import com.almoby.ruralcuruzu.exception.CorreoYaRegistradoException;
import com.almoby.ruralcuruzu.exception.CuitYaRegistradoException;
import com.almoby.ruralcuruzu.repository.BeneficioRepository;
import com.almoby.ruralcuruzu.repository.ComercioEliminadoRepository;
import com.almoby.ruralcuruzu.repository.ComercioRepository;
import com.almoby.ruralcuruzu.repository.HistorialBeneficioRepository;
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
    @Mock
    private BeneficioRepository beneficioRepository;
    @Mock
    private HistorialBeneficioRepository historialBeneficioRepository;
    @Mock
    private ComercioEliminadoRepository comercioEliminadoRepository;

    private ComercioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ComercioServiceImpl(comercioRepository, usuarioRepository, cuentaAccesoService, emailService,
                beneficioRepository, historialBeneficioRepository, comercioEliminadoRepository);
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

    private Comercio comercioExistente() {
        return Comercio.builder()
                .id("comercio-1")
                .nombreComercial("Almacén Don José")
                .razonSocial("Don José S.R.L.")
                .cuit("30-71234567-9")
                .rubro("Almacén y despensa")
                .telefono("+54 9 3777123456")
                .correoElectronico("contacto@donjose.com")
                .direccion("Ruta 123 km 4")
                .estado(EstadoComercio.ACTIVO)
                .usuarioId("usuario-1")
                .fechaAlta(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
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
        assertThat(response.comercio().promociones()).isEmpty();
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
    void obtenerComercioPorId_traeSusPromocionesConUsosDelMesActual() {
        Comercio comercio = comercioExistente();
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio));

        Beneficio beneficio = Beneficio.builder().id("beneficio-1").comercioId("comercio-1")
                .titulo("Café con leche gratis").estado(EstadoBeneficio.ACTIVO).build();
        when(beneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of(beneficio));

        HistorialBeneficio usoEsteMes = HistorialBeneficio.builder()
                .beneficioId("beneficio-1").comercioId("comercio-1")
                .estado(EstadoUsoBeneficio.USADO).fechaUso(Instant.now()).build();
        HistorialBeneficio usoMesPasado = HistorialBeneficio.builder()
                .beneficioId("beneficio-1").comercioId("comercio-1")
                .estado(EstadoUsoBeneficio.USADO).fechaUso(Instant.now().minusSeconds(60L * 60 * 24 * 60)).build();
        when(historialBeneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of(usoEsteMes, usoMesPasado));

        ComercioResponse response = service.obtenerComercioPorId("comercio-1");

        assertThat(response.promociones()).hasSize(1);
        assertThat(response.promociones().get(0).titulo()).isEqualTo("Café con leche gratis");
        assertThat(response.promociones().get(0).usosEsteMes()).isEqualTo(1);
    }

    @Test
    void listarComercios_sinFiltroDeEstado_usaFindAll() {
        Comercio comercio = Comercio.builder().id("comercio-1").estado(EstadoComercio.ACTIVO).build();
        when(comercioRepository.findAll()).thenReturn(List.of(comercio));
        when(beneficioRepository.findAll()).thenReturn(List.of());
        when(historialBeneficioRepository.findAll()).thenReturn(List.of());

        List<ComercioResumenResponse> resultado = service.listarComercios(null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).cantidadPromociones()).isZero();
        assertThat(resultado.get(0).consumosTotales()).isZero();
        verify(comercioRepository, never()).findByEstado(any());
    }

    @Test
    void listarComercios_conFiltroDeEstado_usaFindByEstado() {
        Comercio comercio = Comercio.builder().id("comercio-1").estado(EstadoComercio.ACTIVO).build();
        when(comercioRepository.findByEstado(EstadoComercio.ACTIVO)).thenReturn(List.of(comercio));
        when(beneficioRepository.findAll()).thenReturn(List.of());
        when(historialBeneficioRepository.findAll()).thenReturn(List.of());

        List<ComercioResumenResponse> resultado = service.listarComercios(EstadoComercio.ACTIVO);

        assertThat(resultado).hasSize(1);
        verify(comercioRepository, never()).findAll();
    }

    @Test
    void listarComercios_calculaCantidadPromocionesYConsumosPorComercio() {
        Comercio comercio = Comercio.builder().id("comercio-1").estado(EstadoComercio.ACTIVO).build();
        when(comercioRepository.findAll()).thenReturn(List.of(comercio));
        when(beneficioRepository.findAll()).thenReturn(List.of(
                Beneficio.builder().id("b1").comercioId("comercio-1").build(),
                Beneficio.builder().id("b2").comercioId("comercio-1").build()));
        when(historialBeneficioRepository.findAll()).thenReturn(List.of(
                HistorialBeneficio.builder().comercioId("comercio-1").estado(EstadoUsoBeneficio.USADO).build(),
                HistorialBeneficio.builder().comercioId("comercio-1").estado(EstadoUsoBeneficio.ANULADO).build()));

        List<ComercioResumenResponse> resultado = service.listarComercios(null);

        assertThat(resultado.get(0).cantidadPromociones()).isEqualTo(2);
        // Solo cuenta el USADO: el ANULADO no suma a consumosTotales.
        assertThat(resultado.get(0).consumosTotales()).isEqualTo(1);
    }

    @Test
    void actualizarComercioParcial_conUnSoloCampo_soloModificaEse() {
        Comercio comercio = comercioExistente();
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio));
        when(beneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of());
        when(historialBeneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of());

        ActualizarComercioParcialRequest request = new ActualizarComercioParcialRequest(
                "Farmacia del sol", null, null, null, null, null, null, null, null);

        ComercioActualizadoResponse response = service.actualizarComercioParcial("comercio-1", request);

        assertThat(response.comercio().nombreComercial()).isEqualTo("Farmacia del sol");
        // El resto queda exactamente como estaba.
        assertThat(response.comercio().razonSocial()).isEqualTo("Don José S.R.L.");
        assertThat(response.comercio().cuit()).isEqualTo("30-71234567-9");
        assertThat(response.comercio().correoElectronico()).isEqualTo("contacto@donjose.com");
        verify(usuarioRepository, never()).findById(anyString());
    }

    @Test
    void actualizarComercioParcial_todoNuloOEnBlanco_noCambiaNada() {
        Comercio comercio = comercioExistente();
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio));
        when(beneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of());
        when(historialBeneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of());

        ActualizarComercioParcialRequest request = new ActualizarComercioParcialRequest(
                null, "  ", null, null, null, null, null, null, null);

        ComercioActualizadoResponse response = service.actualizarComercioParcial("comercio-1", request);

        assertThat(response.comercio().nombreComercial()).isEqualTo("Almacén Don José");
        assertThat(response.comercio().razonSocial()).isEqualTo("Don José S.R.L.");
    }

    @Test
    void actualizarComercioParcial_cambiaSoloElCorreo_sincronizaElUsuario() {
        Comercio comercio = comercioExistente();
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio));
        when(usuarioRepository.existsByEmail("nuevo@donjose.com")).thenReturn(false);
        when(comercioRepository.existsByCorreoElectronicoIgnoreCaseAndIdNot("nuevo@donjose.com", "comercio-1"))
                .thenReturn(false);
        when(beneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of());
        when(historialBeneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of());

        Usuario usuarioVinculado = Usuario.builder().id("usuario-1").email("contacto@donjose.com").build();
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(usuarioVinculado));

        ActualizarComercioParcialRequest request = new ActualizarComercioParcialRequest(
                null, null, null, null, null, "nuevo@donjose.com", null, null, null);

        service.actualizarComercioParcial("comercio-1", request);

        assertThat(usuarioVinculado.getEmail()).isEqualTo("nuevo@donjose.com");
        verify(usuarioRepository).save(usuarioVinculado);
    }

    @Test
    void actualizarComercioParcial_conCuitDeOtroComercio_lanzaExcepcionYNoGuardaNada() {
        Comercio comercio = comercioExistente();
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio));
        when(comercioRepository.existsByCuitAndIdNot("30-99999999-1", "comercio-1")).thenReturn(true);

        ActualizarComercioParcialRequest request = new ActualizarComercioParcialRequest(
                null, null, "30-99999999-1", null, null, null, null, null, null);

        assertThatThrownBy(() -> service.actualizarComercioParcial("comercio-1", request))
                .isInstanceOf(CuitYaRegistradoException.class);
        verify(comercioRepository, never()).save(any());
    }

    @Test
    void actualizarComercioParcial_inexistente_lanzaExcepcion() {
        when(comercioRepository.findById("no-existe")).thenReturn(Optional.empty());

        ActualizarComercioParcialRequest request = new ActualizarComercioParcialRequest(
                "Nuevo nombre", null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.actualizarComercioParcial("no-existe", request))
                .isInstanceOf(ComercioNoEncontradoException.class);
    }

    @Test
    void eliminarComercio_borraFisicamenteYDejaConstanciaEnElHistorial() {
        Comercio comercio = comercioExistente();
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio));

        EliminarComercioResponse response = service.eliminarComercio(
                "comercio-1", new EliminarComercioRequest("Cerró el local"), "admin-1", "Admin Uno");

        assertThat(response.mensaje()).isEqualTo("Comercio eliminado correctamente");
        assertThat(response.comercio().nombreComercial()).isEqualTo("Almacén Don José");
        assertThat(response.comercio().motivo()).isEqualTo("Cerró el local");
        assertThat(response.comercio().estadoAlEliminar()).isEqualTo(EstadoComercio.ACTIVO);

        verify(comercioEliminadoRepository).save(any(ComercioEliminado.class));
        verify(beneficioRepository).deleteByComercioId("comercio-1");
        verify(usuarioRepository).deleteById("usuario-1");
        verify(comercioRepository).deleteById("comercio-1");
        verify(emailService).enviarCorreoComercioEliminado("contacto@donjose.com", "Almacén Don José", "Cerró el local");
    }

    @Test
    void eliminarComercio_sinUsuarioVinculado_noIntentaBorrarUsuario() {
        Comercio comercio = comercioExistente();
        comercio.setUsuarioId(null);
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio));

        service.eliminarComercio("comercio-1", new EliminarComercioRequest("Cerró el local"), "admin-1", "Admin Uno");

        verify(usuarioRepository, never()).deleteById(anyString());
    }

    @Test
    void eliminarComercio_inexistente_lanzaExcepcion() {
        when(comercioRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminarComercio(
                "no-existe", new EliminarComercioRequest("motivo"), "admin-1", "Admin Uno"))
                .isInstanceOf(ComercioNoEncontradoException.class);

        verify(comercioEliminadoRepository, never()).save(any());
    }

    @Test
    void listarComerciosEliminados_devuelveElHistorialOrdenado() {
        ComercioEliminado eliminado = ComercioEliminado.builder()
                .id("eliminado-1").nombreComercial("Café San Telmo").fechaBaja(Instant.now()).build();
        when(comercioEliminadoRepository.findAllByOrderByFechaBajaDesc()).thenReturn(List.of(eliminado));

        List<ComercioEliminadoResponse> resultado = service.listarComerciosEliminados();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombreComercial()).isEqualTo("Café San Telmo");
    }
}
