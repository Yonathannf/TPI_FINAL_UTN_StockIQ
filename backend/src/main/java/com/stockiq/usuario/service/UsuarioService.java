package com.stockiq.usuario.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockiq.common.exception.ConflictException;
import com.stockiq.common.exception.NotFoundException;
import com.stockiq.usuario.dto.UsuarioRequest;
import com.stockiq.usuario.dto.UsuarioResponse;
import com.stockiq.usuario.dto.UsuarioUpdateRequest;
import com.stockiq.usuario.mapper.UsuarioMapper;
import com.stockiq.usuario.model.Rol;
import com.stockiq.usuario.model.Usuario;
import com.stockiq.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Gestión de usuarios (RF-03). Aplica RN-33 (username único) y RN-40 (siempre debe quedar al menos
 * un ENCARGADO activo y nadie puede desactivarse ni cambiarse el rol a sí mismo).
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Sort ORDEN = Sort.by("apellido", "nombre");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper mapper;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Boolean activo) {
        List<Usuario> usuarios = activo == null
                ? usuarioRepository.findAll(ORDEN)
                : usuarioRepository.findByActivo(activo, ORDEN);
        return usuarios.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return mapper.toResponse(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        String username = request.username().trim();
        if (usuarioRepository.existsByUsername(username)) {
            throw new ConflictException("USERNAME_DUPLICADO",
                    "Ya existe un usuario con el nombre de usuario '" + username + "'.");
        }
        Usuario nuevo = mapper.toEntity(request, username, passwordEncoder.encode(request.password()));
        return mapper.toResponse(usuarioRepository.save(nuevo));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request, Long usuarioActualId) {
        Usuario usuario = buscar(id);

        if (usuario.getRol() != request.rol()) {
            if (id.equals(usuarioActualId)) {
                throw new ConflictException("CAMBIO_DE_ROL_PROPIO", "No podés cambiar tu propio rol.");
            }
            if (usuario.getRol() == Rol.ENCARGADO && request.rol() == Rol.OPERARIO
                    && usuario.isActivo() && esElUltimoEncargadoActivo()) {
                throw new ConflictException("ULTIMO_ENCARGADO",
                        "No se puede pasar a OPERARIO al último ENCARGADO activo del sistema.");
            }
            usuario.setRol(request.rol());
        }

        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        if (request.password() != null) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return mapper.toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse desactivar(Long id, Long usuarioActualId) {
        Usuario usuario = buscar(id);

        if (id.equals(usuarioActualId)) {
            throw new ConflictException("AUTODESACTIVACION", "No podés desactivar tu propio usuario.");
        }
        if (usuario.isActivo() && usuario.getRol() == Rol.ENCARGADO && esElUltimoEncargadoActivo()) {
            throw new ConflictException("ULTIMO_ENCARGADO",
                    "No se puede desactivar al último ENCARGADO activo del sistema.");
        }
        usuario.setActivo(false);
        return mapper.toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse activar(Long id) {
        Usuario usuario = buscar(id);
        usuario.setActivo(true);
        return mapper.toResponse(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> NotFoundException.of("Usuario", id));
    }

       /** Bloquea a los ENCARGADO activos antes de contarlos, así dos bajas simultáneas no pasan las dos (RN-40). */
    private boolean esElUltimoEncargadoActivo() {
        return usuarioRepository.bloquearActivosPorRol(Rol.ENCARGADO).size() <= 1;
    }
}
