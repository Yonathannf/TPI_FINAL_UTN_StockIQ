package com.stockiq.usuario.mapper;

import org.springframework.stereotype.Component;

import com.stockiq.usuario.dto.UsuarioRequest;
import com.stockiq.usuario.dto.UsuarioResponse;
import com.stockiq.usuario.model.Usuario;

@Component
public class UsuarioMapper {

    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getRol(),
                usuario.isActivo());
    }

    /** El username ya llega normalizado y la contraseña ya hasheada: el mapper no decide eso. */
    public Usuario toEntity(UsuarioRequest request, String username, String passwordHash) {
        return Usuario.builder()
                .username(username)
                .passwordHash(passwordHash)
                .nombre(request.nombre().trim())
                .apellido(request.apellido().trim())
                .rol(request.rol())
                .activo(true)
                .build();
    }
}
