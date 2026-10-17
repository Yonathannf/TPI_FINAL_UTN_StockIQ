package com.stockiq.usuario.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockiq.security.UsuarioActual;
import com.stockiq.usuario.dto.LoginRequest;
import com.stockiq.usuario.dto.LoginResponse;
import com.stockiq.usuario.dto.SesionUsuario;
import com.stockiq.usuario.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Cualquier usuario autenticado (ENCARGADO u OPERARIO). */
    @GetMapping("/me")
    public SesionUsuario me(@AuthenticationPrincipal UsuarioActual actual) {
        return authService.perfil(actual.id());
    }
}
