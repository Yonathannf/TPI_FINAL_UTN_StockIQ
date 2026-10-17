package com.stockiq.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Mismo criterio que application.yml: un campo desconocido en el JSON es un error (RN-41).
        MappingJackson2HttpMessageConverter conversor = new MappingJackson2HttpMessageConverter(
                Jackson2ObjectMapperBuilder.json().failOnUnknownProperties(true).build());
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(conversor)
                .build();
    }

    @Test
    void conflicto_respondeConElFormatoDelContrato() throws Exception {
        mockMvc.perform(get("/prueba/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.mensaje").value("El depósito origen tiene 12,00 un disponibles y se intentan mover 20,00."))
                .andExpect(jsonPath("$.ruta").value("/prueba/conflicto"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void noEncontrado_respondeConElFormatoDelContrato() throws Exception {
        mockMvc.perform(get("/prueba/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("Producto con id 99 no encontrado."));
    }

    @Test
    void solicitudInvalida_respondeConElFormatoDelContrato() throws Exception {
        mockMvc.perform(get("/prueba/solicitud-invalida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("MOTIVO_OBLIGATORIO"))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void validacionDeCampos_responde400ConElDetalleDelCampo() throws Exception {
        mockMvc.perform(post("/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.mensaje").value("nombre: no debe estar vacío"))
                .andExpect(jsonPath("$.errores.nombre").value("no debe estar vacío"));
    }

    @Test
    void campoDesconocido_responde400_RN41() throws Exception {
        mockMvc.perform(post("/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"x\", \"fecha\": \"2026-01-01T10:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CAMPO_NO_PERMITIDO"))
                .andExpect(jsonPath("$.mensaje").value("El campo 'fecha' no se admite en este pedido."));
    }

    @Test
    void jsonMalFormado_responde400() throws Exception {
        mockMvc.perform(post("/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CUERPO_INVALIDO"));
    }

    @Test
    void metodoNoPermitido_respondeConElMismoFormato() throws Exception {
        mockMvc.perform(post("/prueba/conflicto"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"))
                .andExpect(jsonPath("$.ruta").value("/prueba/conflicto"));
    }

    @Test
    void errorInesperado_responde500SinFiltrarElDetalleInterno() throws Exception {
        mockMvc.perform(get("/prueba/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error inesperado. Probá de nuevo más tarde."));
    }

    @RestController
    static class ControladorDePrueba {

        @GetMapping("/prueba/conflicto")
        void conflicto() {
            throw new ConflictException("STOCK_INSUFICIENTE",
                    "El depósito origen tiene 12,00 un disponibles y se intentan mover 20,00.");
        }

        @GetMapping("/prueba/no-encontrado")
        void noEncontrado() {
            throw NotFoundException.of("Producto", 99);
        }

        @GetMapping("/prueba/solicitud-invalida")
        void solicitudInvalida() {
            throw new BadRequestException("MOTIVO_OBLIGATORIO", "El motivo es obligatorio en las salidas.");
        }

        @GetMapping("/prueba/error")
        void error() {
            throw new IllegalStateException("detalle interno que no debe llegar al cliente");
        }

        @PostMapping("/prueba/validacion")
        void validacion(@Valid @RequestBody DatosDePrueba datos) {
            // no hace nada: solo interesa la validación
        }
    }

    record DatosDePrueba(@NotBlank(message = "no debe estar vacío") String nombre) {
    }
}
