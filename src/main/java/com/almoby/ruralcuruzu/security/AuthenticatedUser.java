package com.almoby.ruralcuruzu.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.almoby.ruralcuruzu.domain.Usuario;

/**
 * Adapta un {@link Usuario} al contrato {@link UserDetails} que espera Spring Security,
 * sin ensuciar el documento de dominio con detalles de autenticación.
 */
public class AuthenticatedUser implements UserDetails {

    private final Usuario usuario;

    public AuthenticatedUser(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario usuario() {
        return usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
    }

    @Override
    public String getPassword() {
        return usuario.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return usuario.estaActivo();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.estaActivo();
    }
}
