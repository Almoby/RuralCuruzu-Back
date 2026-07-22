package com.almoby.ruralcuruzu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.almoby.ruralcuruzu.security.RestAccessDeniedHandler;
import com.almoby.ruralcuruzu.security.RestAuthenticationEntryPoint;
import com.almoby.ruralcuruzu.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // OJO: rutas explícitas, no "/api/auth/**". /api/auth/logout debe
    // quedar protegido (requiere Bearer token) para poder revocarlo.
    // Se irán agregando más rutas públicas (ej. alta de solicitud de socio,
    // forgot-password, reset-password) a medida que se implementen esos módulos.
    private static final String[] RUTAS_PUBLICAS = {
            "/api/auth/login",
            "/api/auth/forgot-password",
            "/api/auth/reset-password"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     JwtAuthenticationFilter jwtAuthenticationFilter,
                                                     RestAuthenticationEntryPoint authenticationEntryPoint,
                                                     RestAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
