package com.almoby.ruralcuruzu.security.jwt;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.almoby.ruralcuruzu.service.TokenRevocadoService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Lee el header {@code Authorization: Bearer <token>}, lo valida y,
 * si es correcto (firma OK, no expirado, no revocado), autentica el request
 * contra el SecurityContext de Spring. Si algo falla, simplemente deja pasar
 * el request sin autenticar: es responsabilidad de SecurityConfig (y de
 * RestAuthenticationEntryPoint) decidir qué pasa si esa ruta requería auth.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";
    public static final String ATRIBUTO_TOKEN_ACTUAL = "jwtActual";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenRevocadoService tokenRevocadoService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                    UserDetailsService userDetailsService,
                                    TokenRevocadoService tokenRevocadoService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.tokenRevocadoService = tokenRevocadoService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extraerToken(request);
        String metodoYRuta = request.getMethod() + " " + request.getRequestURI();

        if (token == null) {
            log.debug("{} - sin header Authorization Bearer", metodoYRuta);
        } else if (!jwtService.esValido(token)) {
            log.debug("{} - token presente pero inválido o expirado", metodoYRuta);
        } else if (tokenRevocadoService.estaRevocado(jwtService.extraerJti(token))) {
            log.warn("{} - token con firma válida pero revocado (logout previo)", metodoYRuta);
        } else if (SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticar(request, token, metodoYRuta);
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(HttpServletRequest request, String token, String metodoYRuta) {
        String email = jwtService.extraerEmail(token);
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute(ATRIBUTO_TOKEN_ACTUAL, token);
            log.debug("{} - autenticado como email={}", metodoYRuta, email);
        } catch (UsernameNotFoundException ex) {
            // El token es válido pero el usuario ya no existe (fue dado de baja, por ejemplo).
            // No autenticamos; el request sigue como anónimo y la ruta protegida lo va a rechazar.
            log.warn("{} - token válido pero el usuario email={} ya no existe", metodoYRuta, email);
        }
    }

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIJO_BEARER)) {
            return header.substring(PREFIJO_BEARER.length());
        }
        return null;
    }
}
