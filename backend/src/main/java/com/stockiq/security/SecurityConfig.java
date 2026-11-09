package com.stockiq.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.stockiq.usuario.repository.UsuarioRepository;

/**
 * Autenticación JWT stateless y autorización por rol según docs/06-api-endpoints.md.
 * Las reglas están centralizadas acá; las de módulos que todavía no existen quedan listas.
 */
@Configuration
public class SecurityConfig {

    private static final String ENCARGADO = "ENCARGADO";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            RestAuthenticationEntryPoint entryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(rutas -> rutas
                        // Públicas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // Solo ENCARGADO (RN-34, RN-35)
                        .requestMatchers("/usuarios/**", "/proveedores/**", "/parametros/**", "/analitica/**")
                                .hasRole(ENCARGADO)
                        .requestMatchers("/dashboard/analitica", "/dashboard/evolucion").hasRole(ENCARGADO)
                        // Catálogo y depósitos: lectura para todos, escritura solo ENCARGADO
                        .requestMatchers(HttpMethod.GET, "/categorias/**", "/productos/**", "/depositos/**")
                                .authenticated()
                        .requestMatchers("/categorias/**", "/productos/**", "/depositos/**").hasRole(ENCARGADO)
                        // Movimientos, stock y /dashboard/stock: cualquier usuario autenticado
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, usuarioRepository),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** RNF-01: contraseñas almacenadas con BCrypt. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
