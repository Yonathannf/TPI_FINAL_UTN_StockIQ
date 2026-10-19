package com.stockiq.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.stockiq.common.exception.UnauthorizedException;
import com.stockiq.security.JwtService;
import com.stockiq.usuario.dto.LoginRequest;
import com.stockiq.usuario.dto.LoginResponse;
import com.stockiq.usuario.model.Rol;
import com.stockiq.usuario.model.Usuario;
import com.stockiq.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario(boolean activo) {
        return Usuario.builder().id(1L).username("ana").passwordHash("hash")
                .nombre("Ana").apellido("Paz").rol(Rol.ENCARGADO).activo(activo).build();
    }

    @Test
    void loginCorrecto_devuelveTokenRolYNombre() {
        Usuario ana = usuario(true);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
        when(passwordEncoder.matches("clave", "hash")).thenReturn(true);
        when(jwtService.generarToken(ana)).thenReturn("token-jwt");

        LoginResponse respuesta = authService.login(new LoginRequest(" ana ", "clave"));

        assertThat(respuesta.token()).isEqualTo("token-jwt");
        assertThat(respuesta.usuario().id()).isEqualTo(1L);
        assertThat(respuesta.usuario().username()).isEqualTo("ana");
        assertThat(respuesta.usuario().nombre()).isEqualTo("Ana");
        assertThat(respuesta.usuario().apellido()).isEqualTo("Paz");
        assertThat(respuesta.usuario().rol()).isEqualTo(Rol.ENCARGADO);
    }

    @Test
    void perfil_devuelveLosDatosDelUsuarioActivo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(true)));

        assertThat(authService.perfil(1L).username()).isEqualTo("ana");
    }

    @Test
    void perfil_deUsuarioDesactivadoOInexistente_responde401() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(false)));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.perfil(1L)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> authService.perfil(2L)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void claveIncorrecta_responde401YNoEmiteToken() {
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario(true)));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana", "mala")))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("codigo").isEqualTo("CREDENCIALES_INVALIDAS");
        verifyNoInteractions(jwtService);
    }

    @Test
    void usuarioInactivo_responde401SinSiquieraComprobarLaClave() {
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario(false)));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana", "clave")))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("codigo").isEqualTo("CREDENCIALES_INVALIDAS");
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void usuarioInexistente_responde401() {
        when(usuarioRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nadie", "clave")))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("codigo").isEqualTo("CREDENCIALES_INVALIDAS");
        verifyNoInteractions(passwordEncoder, jwtService);
    }
}
