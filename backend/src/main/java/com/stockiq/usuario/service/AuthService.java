package com.stockiq.usuario.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockiq.common.exception.UnauthorizedException;
import com.stockiq.security.JwtService;
import com.stockiq.usuario.dto.LoginRequest;
import com.stockiq.usuario.dto.LoginResponse;
import com.stockiq.usuario.dto.SesionUsuario;
import com.stockiq.usuario.model.Usuario;
import com.stockiq.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/** RF-01: inicio de sesión con usuario y contraseña. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.username().trim())
                .filter(Usuario::isActivo)
                .orElseThrow(UnauthorizedException::credencialesInvalidas);

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw UnauthorizedException.credencialesInvalidas();
        }

        return new LoginResponse(jwtService.generarToken(usuario), SesionUsuario.de(usuario));
    }

    /** GET /auth/me: el front lo usa al recargar la página para recuperar la sesión a partir del token. */
    @Transactional(readOnly = true)
    public SesionUsuario perfil(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .filter(Usuario::isActivo)
                .map(SesionUsuario::de)
                .orElseThrow(UnauthorizedException::credencialesInvalidas);
    }
}
