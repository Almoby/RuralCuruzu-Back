package com.almoby.ruralcuruzu.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.almoby.ruralcuruzu.constantes.RutasApi;
import com.almoby.ruralcuruzu.security.RestAccessDeniedHandler;
import com.almoby.ruralcuruzu.security.RestAuthenticationEntryPoint;
import com.almoby.ruralcuruzu.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // OJO: rutas explícitas, no "/api/auth/**". /api/auth/logout debe
    // quedar protegido (requiere Bearer token) para poder revocarlo.
    // Se irán agregando más rutas públicas a medida que se implementen esos módulos.
    private static final String[] RUTAS_PUBLICAS = {
            RutasApi.LOGIN,
            RutasApi.FORGOT_PASSWORD,
            RutasApi.RESET_PASSWORD,
            RutasApi.REFRESH,
            // Botón "Quiero ser socio": cualquier visitante puede enviar una solicitud.
            RutasApi.SOLICITUDES_SOCIO,
            // Documentación Swagger/OpenAPI: pública para poder verla sin loguearse.
            RutasApi.SWAGGER_UI_HTML,
            RutasApi.SWAGGER_UI,
            RutasApi.API_DOCS
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Sin esto, el navegador bloquea las llamadas del frontend (en otro
     * origen: otro puerto o dominio) apenas intente pegarle a esta API,
     * aunque el backend responda bien. Los orígenes permitidos son
     * configurables porque van a cambiar entre desarrollo y producción
     * (ej. el dominio real del frontend una vez desplegado).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:4200}") String origenesPermitidos) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(Arrays.asList(origenesPermitidos.split(",")));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // false porque la autenticación viaja en el header Authorization (JWT),
        // no en cookies: no hace falta que el navegador mande credenciales.
        configuracion.setAllowCredentials(false);
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuracion);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     JwtAuthenticationFilter jwtAuthenticationFilter,
                                                     RestAuthenticationEntryPoint authenticationEntryPoint,
                                                     RestAccessDeniedHandler accessDeniedHandler,
                                                     CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .requestMatchers(RutasApi.ADMIN_SOLICITUDES_SOCIO).hasRole("ADMIN")
                        .requestMatchers(RutasApi.ADMIN_COMERCIOS).hasRole("ADMIN")
                        .requestMatchers(RutasApi.ADMIN_TIPOS_CUOTA).hasRole("ADMIN")
                        .requestMatchers(RutasApi.ADMIN_CUOTAS).hasRole("ADMIN")
                        .requestMatchers(RutasApi.SOCIO_CUOTAS).hasRole("SOCIO")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
