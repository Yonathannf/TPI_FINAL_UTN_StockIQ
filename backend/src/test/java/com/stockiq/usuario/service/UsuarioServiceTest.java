package com.stockiq.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.stockiq.common.exception.ConflictException;
import com.stockiq.common.exception.NotFoundException;
import com.stockiq.usuario.dto.UsuarioRequest;
import com.stockiq.usuario.dto.UsuarioResponse;
import com.stockiq.usuario.dto.UsuarioUpdateRequest;
import com.stockiq.usuario.mapper.UsuarioMapper;
import com.stockiq.usuario.model.Rol;
import com.stockiq.usuario.model.Usuario;
import com.stockiq.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final Long YO = 1L;
    private static final Long OTRO = 2L;

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService servicio;

    @BeforeEach
    void setUp() {
        servicio = new UsuarioService(usuarioRepository, passwordEncoder, new UsuarioMapper());
    }

    private Usuario usuario(Long id, Rol rol, boolean activo) {
        return Usuario.builder().id(id).username("user" + id).passwordHash("hash-original")
                .nombre("Nombre").apellido("Apellido").rol(rol).activo(activo).build();
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crear_hasheaLaClaveYDejaElUsuarioActivo() {
        when(usuarioRepository.existsByUsername("nueva")).thenReturn(false);
        when(passwordEncoder.encode("clave1234")).thenReturn("hash-nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        UsuarioResponse respuesta = servicio.crear(
                new UsuarioRequest(" nueva ", "clave1234", " Ana ", "Paz", Rol.OPERARIO));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getPasswordHash()).isEqualTo("hash-nuevo");
        assertThat(respuesta.id()).isEqualTo(10L);
        assertThat(respuesta.username()).isEqualTo("nueva");
        assertThat(respuesta.nombre()).isEqualTo("Ana");
        assertThat(respuesta.activo()).isTrue();
    }

    @Test
    void crear_conUsernameRepetido_responde409_RN33() {
        when(usuarioRepository.existsByUsername("ana")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(new UsuarioRequest("ana", "clave1234", "Ana", "Paz", Rol.OPERARIO)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("USERNAME_DUPLICADO");
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    // --------------------------------------------------------------- actualizar

    @Test
    void actualizar_cambiarElPropioRol_responde409_RN40() {
        Usuario yo = usuario(YO, Rol.ENCARGADO, true);
        when(usuarioRepository.findById(YO)).thenReturn(Optional.of(yo));

        assertThatThrownBy(() -> servicio.actualizar(YO,
                new UsuarioUpdateRequest("A", "B", Rol.OPERARIO, null), YO))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("CAMBIO_DE_ROL_PROPIO");
        assertThat(yo.getRol()).isEqualTo(Rol.ENCARGADO);
    }

    @Test
    void actualizar_pasarAOperarioAlUltimoEncargadoActivo_responde409_RN40() {
        Usuario ultimo = usuario(OTRO, Rol.ENCARGADO, true);
        when(usuarioRepository.findById(OTRO)).thenReturn(Optional.of(ultimo));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ENCARGADO)).thenReturn(1L);

        assertThatThrownBy(() -> servicio.actualizar(OTRO,
                new UsuarioUpdateRequest("A", "B", Rol.OPERARIO, null), YO))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("ULTIMO_ENCARGADO");
        assertThat(ultimo.getRol()).isEqualTo(Rol.ENCARGADO);
    }

    @Test
    void actualizar_pasarAOperarioUnEncargadoCuandoHayOtroActivo_funciona() {
        Usuario encargado = usuario(OTRO, Rol.ENCARGADO, true);
        when(usuarioRepository.findById(OTRO)).thenReturn(Optional.of(encargado));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ENCARGADO)).thenReturn(2L);

        UsuarioResponse respuesta = servicio.actualizar(OTRO,
                new UsuarioUpdateRequest("Nuevo", "Nombre", Rol.OPERARIO, null), YO);

        assertThat(respuesta.rol()).isEqualTo(Rol.OPERARIO);
        assertThat(respuesta.nombre()).isEqualTo("Nuevo");
    }

    @Test
    void actualizar_sinPassword_noTocaElHash() {
        Usuario operario = usuario(OTRO, Rol.OPERARIO, true);
        when(usuarioRepository.findById(OTRO)).thenReturn(Optional.of(operario));

        servicio.actualizar(OTRO, new UsuarioUpdateRequest("A", "B", Rol.OPERARIO, null), YO);

        assertThat(operario.getPasswordHash()).isEqualTo("hash-original");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void actualizar_conPassword_laHashea() {
        Usuario operario = usuario(OTRO, Rol.OPERARIO, true);
        when(usuarioRepository.findById(OTRO)).thenReturn(Optional.of(operario));
        when(passwordEncoder.encode("clave-nueva-1")).thenReturn("hash-nuevo");

        servicio.actualizar(OTRO, new UsuarioUpdateRequest("A", "B", Rol.OPERARIO, "clave-nueva-1"), YO);

        assertThat(operario.getPasswordHash()).isEqualTo("hash-nuevo");
    }

    // --------------------------------------------------------- desactivar / activar

    @Test
    void desactivar_aSiMismo_responde409_RN40() {
        Usuario yo = usuario(YO, Rol.ENCARGADO, true);
        when(usuarioRepository.findById(YO)).thenReturn(Optional.of(yo));

        assertThatThrownBy(() -> servicio.desactivar(YO, YO))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("AUTODESACTIVACION");
        assertThat(yo.isActivo()).isTrue();
    }

    @Test
    void desactivar_alUltimoEncargadoActivo_responde409_RN40() {
        Usuario ultimo = usuario(OTRO, Rol.ENCARGADO, true);
        when(usuarioRepository.findById(OTRO)).thenReturn(Optional.of(ultimo));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ENCARGADO)).thenReturn(1L);

        assertThatThrownBy(() -> servicio.desactivar(OTRO, YO))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("ULTIMO_ENCARGADO");
        assertThat(ultimo.isActivo()).isTrue();
    }

    @Test
    void desactivar_aUnOperario_funcionaSinMirarLaCuentaDeEncargados() {
        Usuario operario = usuario(OTRO, Rol.OPERARIO, true);
        when(usuarioRepository.findById(OTRO)).thenReturn(Optional.of(operario));

        UsuarioResponse respuesta = servicio.desactivar(OTRO, YO);

        assertThat(respuesta.activo()).isFalse();
        assertThat(operario.isActivo()).isFalse();
    }

    @Test
    void activar_reactivaAlUsuario() {
        Usuario inactivo = usuario(OTRO, Rol.OPERARIO, false);
        when(usuarioRepository.findById(OTRO)).thenReturn(Optional.of(inactivo));

        UsuarioResponse respuesta = servicio.activar(OTRO);

        assertThat(respuesta.activo()).isTrue();
    }

    @Test
    void obtener_deUnIdInexistente_responde404() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(99L)).isInstanceOf(NotFoundException.class);
    }
}
