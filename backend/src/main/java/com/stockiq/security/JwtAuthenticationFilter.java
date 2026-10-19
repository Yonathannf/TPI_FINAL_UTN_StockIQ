package com.stockiq.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.stockiq.usuario.model.Usuario;
import com.stockiq.usuario.repository.UsuarioRepository;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Lee el header {@code Authorization: Bearer <token>} y autentica el pedido.
 *
 * El usuario y su rol se consultan en la base en cada pedido (RF-02): si se lo desactivó, pierde el acceso
 * de inmediato aunque su token siga vigente, y un cambio de rol rige desde el siguiente pedido.
 * Si el token falta o no sirve, el pedido sigue sin autenticar y SecurityConfig responde 401.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (encabezado != null && encabezado.startsWith(PREFIJO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = jwtService.extraerUsername(encabezado.substring(PREFIJO.length()));
                usuarioRepository.findByUsername(username)
                        .filter(Usuario::isActivo)
                        .ifPresent(this::autenticar);
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Token rechazado: {}", ex.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private void autenticar(Usuario usuario) {
        UsuarioActual principal = new UsuarioActual(usuario.getId(), usuario.getUsername(), usuario.getRol());
        var autenticacion = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
