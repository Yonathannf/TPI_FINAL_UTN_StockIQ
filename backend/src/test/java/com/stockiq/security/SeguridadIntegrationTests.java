package com.stockiq.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockiq.AbstractIntegrationTest;
import com.stockiq.usuario.model.Rol;
import com.stockiq.usuario.model.Usuario;
import com.stockiq.usuario.repository.UsuarioRepository;

/** Login, 401 y 403 contra la aplicación completa (RF-01, RF-02, RN-34, RN-35). */
@AutoConfigureMockMvc
class SeguridadIntegrationTests extends AbstractIntegrationTest {

    private static final String CLAVE = "clave-segura-123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    private Usuario crearUsuario(Rol rol) {
        return usuarioRepository.save(Usuario.builder()
                .username("u-" + UUID.randomUUID().toString().substring(0, 8))
                .passwordHash(passwordEncoder.encode(CLAVE))
                .nombre("Nombre").apellido("Apellido")
                .rol(rol).activo(true).build());
    }

    private String loginYObtenerToken(String username, String clave) throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of("username", username, "password", clave));
        String respuesta = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void loginDelEncargadoInicial_devuelveTokenRolYNombre() throws Exception {
        String cuerpo = objectMapper.writeValueAsString(
                Map.of("username", "test-admin", "password", "test-admin-1234"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.id").isNumber())
                .andExpect(jsonPath("$.usuario.rol").value("ENCARGADO"))
                .andExpect(jsonPath("$.usuario.nombre").value("Administrador"));
    }

    @Test
    void authMe_conTokenDevuelveElUsuario_ySinTokenResponde401() throws Exception {
        Usuario operario = crearUsuario(Rol.OPERARIO);
        String token = loginYObtenerToken(operario.getUsername(), CLAVE);

        mockMvc.perform(get("/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(operario.getUsername()))
                .andExpect(jsonPath("$.rol").value("OPERARIO"));

        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginConClaveIncorrecta_responde401() throws Exception {
        Usuario usuario = crearUsuario(Rol.OPERARIO);
        String cuerpo = objectMapper.writeValueAsString(
                Map.of("username", usuario.getUsername(), "password", "clave-equivocada"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void sinToken_responde401ConElFormatoDeError() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.ruta").value("/usuarios"));
    }

    @Test
    void tokenAdulterado_responde401() throws Exception {
        Usuario encargado = crearUsuario(Rol.ENCARGADO);
        String[] partes = loginYObtenerToken(encargado.getUsername(), CLAVE).split("\\.");
        String adulterado = partes[0] + "." + partes[1] + "A." + partes[2];

        mockMvc.perform(get("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(adulterado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operarioEnRutaDeEncargado_responde403() throws Exception {
        Usuario operario = crearUsuario(Rol.OPERARIO);
        String token = loginYObtenerToken(operario.getUsername(), CLAVE);

        mockMvc.perform(get("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void encargado_puedeListarUsuarios() throws Exception {
        Usuario encargado = crearUsuario(Rol.ENCARGADO);
        String token = loginYObtenerToken(encargado.getUsername(), CLAVE);

        mockMvc.perform(get("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void usuarioDesactivadoConTokenVigente_pierdeElAccesoDeInmediato_RF02() throws Exception {
        Usuario encargado = crearUsuario(Rol.ENCARGADO);
        String token = loginYObtenerToken(encargado.getUsername(), CLAVE);
        mockMvc.perform(get("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        encargado.setActivo(false);
        usuarioRepository.save(encargado);

        mockMvc.perform(get("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void healthCheck_esPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
