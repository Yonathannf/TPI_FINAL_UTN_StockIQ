package com.stockiq.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.stockiq.usuario.model.Rol;
import com.stockiq.usuario.model.Usuario;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final String SECRET = "secreto-de-prueba-con-mas-de-32-caracteres-1234";
    private static final String OTRO_SECRET = "otra-clave-distinta-de-mas-de-32-caracteres-99";

    private Usuario usuario() {
        return Usuario.builder().id(1L).username("ana").passwordHash("x")
                .nombre("Ana").apellido("Paz").rol(Rol.OPERARIO).build();
    }

    @Test
    void tokenGeneradoSePuedeLeer() {
        JwtService servicio = new JwtService(SECRET, 60);

        String token = servicio.generarToken(usuario());

        assertThat(servicio.extraerUsername(token)).isEqualTo("ana");
    }

    @Test
    void tokenFirmadoConOtraClaveSeRechaza() {
        String token = new JwtService(SECRET, 60).generarToken(usuario());
        JwtService otroServicio = new JwtService(OTRO_SECRET, 60);

        assertThatThrownBy(() -> otroServicio.extraerUsername(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenVencidoSeRechaza() {
        JwtService servicio = new JwtService(SECRET, -1);
        String token = servicio.generarToken(usuario());

        assertThatThrownBy(() -> servicio.extraerUsername(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenAdulteradoSeRechaza() {
        JwtService servicio = new JwtService(SECRET, 60);
        String[] partes = servicio.generarToken(usuario()).split("\\.");
        String adulterado = partes[0] + "." + partes[1] + "A." + partes[2];

        assertThatThrownBy(() -> servicio.extraerUsername(adulterado)).isInstanceOf(JwtException.class);
    }

    @Test
    void secretDemasiadoCortoSeRechazaAlArrancar() {
        assertThatThrownBy(() -> new JwtService("corto", 60)).isInstanceOf(IllegalStateException.class);
    }
}
