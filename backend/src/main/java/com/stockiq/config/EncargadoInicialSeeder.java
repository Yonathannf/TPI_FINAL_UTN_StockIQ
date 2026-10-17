package com.stockiq.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.stockiq.usuario.model.Rol;
import com.stockiq.usuario.model.Usuario;
import com.stockiq.usuario.repository.UsuarioRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Crea el usuario ENCARGADO inicial en el primer arranque (docs/03, sección 7), solo si la tabla
 * de usuarios está vacía. Sin él nadie podría iniciar sesión para crear a los demás.
 */
@Slf4j
@Component
public class EncargadoInicialSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public EncargadoInicialSeeder(UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${stockiq.seed.admin-username:admin}") String username,
            @Value("${stockiq.seed.admin-password:}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "No hay usuarios cargados y falta la contraseña del ENCARGADO inicial. "
                            + "Definí la variable de entorno ADMIN_PASSWORD (stockiq.seed.admin-password).");
        }
        usuarioRepository.save(Usuario.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .nombre("Administrador")
                .apellido("Inicial")
                .rol(Rol.ENCARGADO)
                .activo(true)
                .build());
        log.info("Se creó el usuario ENCARGADO inicial '{}'. Cambiá su contraseña después del primer ingreso.", username);
    }
}
