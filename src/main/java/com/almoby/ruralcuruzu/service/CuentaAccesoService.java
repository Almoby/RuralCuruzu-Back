package com.almoby.ruralcuruzu.service;

import com.almoby.ruralcuruzu.domain.Usuario;
import com.almoby.ruralcuruzu.enums.Rol;

/**
 * Alta de la cuenta de acceso (Usuario) con contraseña temporal para un
 * perfil recién creado (Socio o Comercio): mismo mecanismo en los dos casos,
 * documento secciones 8.4 y 12.3 respectivamente. Centralizado acá para que
 * SocioService y ComercioService no dupliquen la generación de la
 * contraseña temporal ni la construcción del Usuario.
 */
public interface CuentaAccesoService {

    /**
     * Crea y guarda un Usuario con una contraseña temporal recién generada
     * (nunca se persiste en texto plano: solo viaja en el valor de retorno,
     * para que el que llama pueda mandarla por correo).
     *
     * @param email correo con el que va a iniciar sesión (también el destino del correo de credenciales)
     * @param nombre nombre para mostrar, denormalizado en el Usuario
     * @param rol SOCIO o COMERCIO
     * @param refId id del documento Socio o Comercio al que queda vinculada esta cuenta
     */
    CuentaTemporalCreada crearCuentaConPasswordTemporal(String email, String nombre, Rol rol, String refId);

    /**
     * @param usuario ya guardado, con id asignado
     * @param passwordTemporal en texto plano, para mandar por correo (no se guarda así en ningún lado)
     */
    record CuentaTemporalCreada(Usuario usuario, String passwordTemporal) {
    }
}
