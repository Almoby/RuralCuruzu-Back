package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.enums.EstadoUsuario;
import com.almoby.ruralcuruzu.enums.Rol;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.service.CuentaAccesoService;

/**
 * Tests unitarios de la generación de cuentas con contraseña temporal,
 * compartida entre el alta de Socio y de Comercio.
 */
@ExtendWith(MockitoExtension.class)
class CuentaAccesoServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private CuentaAccesoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CuentaAccesoServiceImpl(usuarioRepository, passwordEncoder);
        when(passwordEncoder.encode(anyString())).thenReturn("hash-encriptado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId("usuario-generado-1");
            return usuario;
        });
    }

    @Test
    void crearCuentaConPasswordTemporal_creaElUsuarioConLosDatosCorrectos() {
        CuentaAccesoService.CuentaTemporalCreada cuenta = service.crearCuentaConPasswordTemporal(
                "comercio@ejemplo.com", "Almacén Don José", Rol.COMERCIO, "comercio-id-1");

        Usuario usuario = cuenta.usuario();
        assertThat(usuario.getId()).isEqualTo("usuario-generado-1");
        assertThat(usuario.getEmail()).isEqualTo("comercio@ejemplo.com");
        assertThat(usuario.getNombre()).isEqualTo("Almacén Don José");
        assertThat(usuario.getRol()).isEqualTo(Rol.COMERCIO);
        assertThat(usuario.getRefId()).isEqualTo("comercio-id-1");
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(usuario.isRequiereCambioPassword()).isTrue();
        assertThat(usuario.getPasswordHash()).isEqualTo("hash-encriptado");

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void crearCuentaConPasswordTemporal_devuelveLaPasswordEnTextoPlanoSinGuardarla() {
        CuentaAccesoService.CuentaTemporalCreada cuenta = service.crearCuentaConPasswordTemporal(
                "socio@ejemplo.com", "Juan Pérez", Rol.SOCIO, "socio-id-1");

        assertThat(cuenta.passwordTemporal()).hasSize(10);

        ArgumentCaptor<String> passwordPlanoCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(passwordPlanoCaptor.capture());
        assertThat(passwordPlanoCaptor.getValue()).isEqualTo(cuenta.passwordTemporal());

        // La contraseña en texto plano nunca es la misma string que el hash guardado.
        assertThat(cuenta.usuario().getPasswordHash()).isNotEqualTo(cuenta.passwordTemporal());
    }

    @Test
    void crearCuentaConPasswordTemporal_generaPasswordsDistintasEnCadaLlamada() {
        CuentaAccesoService.CuentaTemporalCreada primera = service.crearCuentaConPasswordTemporal(
                "a@ejemplo.com", "A", Rol.SOCIO, "id-a");
        CuentaAccesoService.CuentaTemporalCreada segunda = service.crearCuentaConPasswordTemporal(
                "b@ejemplo.com", "B", Rol.SOCIO, "id-b");

        assertThat(primera.passwordTemporal()).isNotEqualTo(segunda.passwordTemporal());
    }
}
