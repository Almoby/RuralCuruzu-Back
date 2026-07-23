package com.almoby.ruralcuruzu.service;

import com.almoby.ruralcuruzu.dto.request.LoginRequest;
import com.almoby.ruralcuruzu.dto.response.LoginResponse;

public interface AuthService {

    /**
     * Autentica un usuario (Socio, Comercio o Admin) y devuelve su token.
     *
     * @throws com.almoby.ruralcuruzu.exception.CredencialesInvalidasException si el email no existe
     *         o la contraseña no coincide.
     * @throws com.almoby.ruralcuruzu.exception.UsuarioInactivoException si el usuario existe pero
     *         su cuenta está inactiva o suspendida.
     */
    LoginResponse login(LoginRequest request);

    /**
     * Revoca el access token actual y, si se manda uno, también el refresh token
     * (a partir de este momento ninguno de los dos sirve para autenticar).
     *
     * @param refreshToken puede ser null: es opcional que el cliente lo mande.
     */
    void logout(String token, String refreshToken);

    /**
     * Renueva el access token sin pedirle al usuario que vuelva a loguearse,
     * a partir de un refresh token válido. El refresh token usado queda
     * revocado y se emite uno nuevo (rotación).
     *
     * @throws com.almoby.ruralcuruzu.exception.RefreshTokenInvalidoException si no existe,
     *         ya fue usado, o el usuario dueño ya no existe.
     * @throws com.almoby.ruralcuruzu.exception.RefreshTokenExpiradoException si existe
     *         pero venció.
     * @throws com.almoby.ruralcuruzu.exception.UsuarioInactivoException si el usuario
     *         ya no está activo.
     */
    LoginResponse refrescarToken(String refreshToken);

    /**
     * Inicia el flujo de recuperación de contraseña: si el email existe, genera
     * un token de un solo uso y lo envía por correo. Nunca informa si el email
     * existe o no (la respuesta es idéntica en ambos casos) para no permitir
     * enumerar usuarios registrados.
     */
    void solicitarRecuperacionPassword(String email);

    /**
     * Completa la recuperación de contraseña: valida el token y, si es válido,
     * actualiza la contraseña del usuario dueño de ese token.
     *
     * @throws com.almoby.ruralcuruzu.exception.TokenRecuperacionInvalidoException si el token
     *         no existe o ya fue usado.
     * @throws com.almoby.ruralcuruzu.exception.TokenRecuperacionExpiradoException si el token
     *         existe pero venció.
     * @throws com.almoby.ruralcuruzu.exception.PasswordIgualException si la nueva contraseña
     *         es igual a la actual.
     */
    void restablecerPassword(String token, String nuevaPassword);
}
