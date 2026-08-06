package com.almoby.ruralcuruzu.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.almoby.ruralcuruzu.service.TokenRevocadoService;

/**
 * Punto más sensible del cambio de SSE: extraerToken() acepta el JWT por
 * query param, pero SOLO para la ruta exacta del stream. Estos tests prueban
 * ese gating directamente contra doFilterInternal (protected, mismo paquete),
 * sin necesitar levantar el contexto de Spring completo.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "token-valido";
    private static final String EMAIL = "juan@example.com";

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private TokenRevocadoService tokenRevocadoService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void crearFiltro() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, tokenRevocadoService);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void stubTokenValido() {
        when(jwtService.esValido(TOKEN)).thenReturn(true);
        when(jwtService.extraerJti(TOKEN)).thenReturn("jti-1");
        when(jwtService.extraerEmail(TOKEN)).thenReturn(EMAIL);
        when(tokenRevocadoService.estaRevocado("jti-1")).thenReturn(false);
        UserDetails userDetails = new User(EMAIL, "n/a", java.util.List.of());
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
    }

    @Test
    void queryParamEnRutaDeStream_autentica() throws Exception {
        stubTokenValido();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notificaciones/stream");
        request.setParameter("token", TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(EMAIL);
    }

    @Test
    void queryParamEnOtraRuta_seIgnoraYQuedaSinAutenticar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/socios");
        request.setParameter("token", TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // Nunca debería siquiera intentar validar un token que no llegó por header.
        verifyNoInteractions(jwtService);
    }

    @Test
    void sinTokenEnRutaDeStream_quedaSinAutenticar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notificaciones/stream");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void headerAuthorizationEnRutaDeStream_siguFuncionandoIgualQueSiempre() throws Exception {
        stubTokenValido();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notificaciones/stream");
        request.addHeader("Authorization", "Bearer " + TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void headerYQueryParamPresentes_elHeaderTienePrioridad() throws Exception {
        stubTokenValido();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notificaciones/stream");
        request.addHeader("Authorization", "Bearer " + TOKEN);
        request.setParameter("token", "otro-token-que-no-deberia-usarse");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        // Si hubiera usado el query param, jwtService.esValido se habría llamado con
        // "otro-token-que-no-deberia-usarse", que no está stubbeado (devolvería false
        // por default de Mockito) y la autenticación no se hubiese seteado.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void trailingSlashEnRutaDeStream_noMatcheaYQueryParamSeIgnora() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notificaciones/stream/");
        request.setParameter("token", TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }
}
