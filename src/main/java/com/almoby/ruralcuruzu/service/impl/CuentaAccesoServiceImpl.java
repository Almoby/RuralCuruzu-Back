package com.almoby.ruralcuruzu.service.impl;

import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.enums.EstadoUsuario;
import com.almoby.ruralcuruzu.enums.Rol;
import com.almoby.ruralcuruzu.repository.UsuarioRepository;
import com.almoby.ruralcuruzu.service.CuentaAccesoService;

@Service
public class CuentaAccesoServiceImpl implements CuentaAccesoService {

    /**
     * Sin caracteres ambiguos (0/O, 1/l/I) para que la contraseña temporal,
     * que la persona tiene que tipear a mano en el primer ingreso, no genere dudas.
     */
    private static final String CARACTERES_PASSWORD_TEMPORAL =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LONGITUD_PASSWORD_TEMPORAL = 10;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom generadorAleatorio = new SecureRandom();

    public CuentaAccesoServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public CuentaTemporalCreada crearCuentaConPasswordTemporal(String email, String nombre, Rol rol, String refId) {
        String passwordTemporal = generarPasswordTemporal();

        Usuario usuario = Usuario.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(passwordTemporal))
                .rol(rol)
                .refId(refId)
                .nombre(nombre)
                .estado(EstadoUsuario.ACTIVO)
                .requiereCambioPassword(true)
                .fechaCreacion(Instant.now())
                .build();

        usuarioRepository.save(usuario);

        return new CuentaTemporalCreada(usuario, passwordTemporal);
    }

    private String generarPasswordTemporal() {
        StringBuilder passwordTemporal = new StringBuilder(LONGITUD_PASSWORD_TEMPORAL);
        for (int i = 0; i < LONGITUD_PASSWORD_TEMPORAL; i++) {
            int indice = generadorAleatorio.nextInt(CARACTERES_PASSWORD_TEMPORAL.length());
            passwordTemporal.append(CARACTERES_PASSWORD_TEMPORAL.charAt(indice));
        }
        return passwordTemporal.toString();
    }
}
