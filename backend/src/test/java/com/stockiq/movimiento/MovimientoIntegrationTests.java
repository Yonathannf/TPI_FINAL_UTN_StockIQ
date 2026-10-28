package com.stockiq.movimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockiq.AbstractIntegrationTest;
import com.stockiq.catalogo.model.Categoria;
import com.stockiq.catalogo.model.Producto;
import com.stockiq.catalogo.model.Proveedor;
import com.stockiq.catalogo.repository.CategoriaRepository;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.catalogo.repository.ProveedorRepository;
import com.stockiq.common.exception.ConflictException;
import com.stockiq.deposito.model.Deposito;
import com.stockiq.deposito.repository.DepositoRepository;
import com.stockiq.movimiento.dto.SalidaRequest;
import com.stockiq.movimiento.service.MovimientoService;
import com.stockiq.security.UsuarioActual;
import com.stockiq.usuario.model.Rol;
import com.stockiq.usuario.model.Usuario;
import com.stockiq.usuario.repository.UsuarioRepository;

/**
 * Movimientos y stock de punta a punta contra un MySQL real: registro, ciclo de las transferencias,
 * permisos por rol, formato de la API y bloqueo ante pedidos simultáneos (RN-01 a RN-18, RN-34, D-13).
 */
@AutoConfigureMockMvc
class MovimientoIntegrationTests extends AbstractIntegrationTest {

    private static final String CLAVE = "clave-segura-123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private DepositoRepository depositoRepository;
    @Autowired
    private MovimientoService movimientoService;

    private Usuario encargado;
    private Usuario operario;
    private String tokenEncargado;
    private String tokenOperario;
    private Producto producto;
    private Deposito origen;
    private Deposito destino;

    @BeforeEach
    void setUp() throws Exception {
        encargado = crearUsuario(Rol.ENCARGADO);
        operario = crearUsuario(Rol.OPERARIO);
        tokenEncargado = login(encargado);
        tokenOperario = login(operario);
        producto = crearProducto(false);
        origen = crearDeposito();
        destino = crearDeposito();
    }

    // ------------------------------------------------------------ escenarios

    @Test
    void cicloDeUnaTransferenciaConfirmada_actualizaStockYTransito() throws Exception {
        entrada(tokenOperario, origen, "10");

        JsonNode transferencia = json(enviar(post("/movimientos/transferencias"), tokenOperario, Map.of(
                "productoId", producto.getId(), "depositoOrigenId", origen.getId(),
                "depositoDestinoId", destino.getId(), "cantidad", 4)).andExpect(status().isCreated()));
        assertThat(transferencia.get("estadoTransferencia").asText()).isEqualTo("PENDIENTE");

        // Pendiente: ya descontó del origen, todavía no suma al destino y está en tránsito (RN-10, RN-17).
        JsonNode stock = stock();
        assertThat(cantidadEn(stock, origen)).isEqualByComparingTo("6");
        assertThat(cantidadEn(stock, destino)).isEqualByComparingTo("0");
        assertThat(stock.get("totalDisponible").decimalValue()).isEqualByComparingTo("6");
        assertThat(stock.get("enTransito").decimalValue()).isEqualByComparingTo("4");

        mockMvc.perform(get("/movimientos/transferencias/pendientes").header("Authorization", bearer(tokenOperario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + transferencia.get("id").asLong() + ")]").isNotEmpty());
        mockMvc.perform(get("/stock/transito").header("Authorization", bearer(tokenOperario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.producto.id==" + producto.getId() + ")].enTransito").isNotEmpty());

        // Confirmada: suma al destino y el tránsito vuelve a cero (RN-11).
        enviar(patch("/movimientos/" + transferencia.get("id").asLong() + "/confirmar"), tokenEncargado, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoTransferencia").value("CONFIRMADA"))
                .andExpect(jsonPath("$.fechaResolucion", endsWith("Z")))
                .andExpect(jsonPath("$.usuarioResolucion.id").value(encargado.getId()));

        stock = stock();
        assertThat(cantidadEn(stock, origen)).isEqualByComparingTo("6");
        assertThat(cantidadEn(stock, destino)).isEqualByComparingTo("4");
        assertThat(stock.get("totalDisponible").decimalValue()).isEqualByComparingTo("10");
        assertThat(stock.get("enTransito").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void transferenciaCancelada_devuelveElStockAlOrigen() throws Exception {
        entrada(tokenOperario, origen, "10");
        long id = json(enviar(post("/movimientos/transferencias"), tokenOperario, Map.of(
                "productoId", producto.getId(), "depositoOrigenId", origen.getId(),
                "depositoDestinoId", destino.getId(), "cantidad", 3)).andExpect(status().isCreated()))
                .get("id").asLong();

        enviar(patch("/movimientos/" + id + "/cancelar"), tokenEncargado, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoTransferencia").value("CANCELADA"));

        JsonNode stock = stock();
        assertThat(cantidadEn(stock, origen)).isEqualByComparingTo("10");
        assertThat(cantidadEn(stock, destino)).isEqualByComparingTo("0");
        assertThat(stock.get("enTransito").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void transferenciaResuelta_noSePuedeResolverDeNuevo() throws Exception {
        entrada(tokenOperario, origen, "10");
        long id = json(enviar(post("/movimientos/transferencias"), tokenOperario, Map.of(
                "productoId", producto.getId(), "depositoOrigenId", origen.getId(),
                "depositoDestinoId", destino.getId(), "cantidad", 2)).andExpect(status().isCreated()))
                .get("id").asLong();
        enviar(patch("/movimientos/" + id + "/confirmar"), tokenEncargado, null).andExpect(status().isOk());

        enviar(patch("/movimientos/" + id + "/cancelar"), tokenEncargado, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSFERENCIA_NO_PENDIENTE"));
    }

    @Test
    void operario_registraMovimientosPeroNoResuelveTransferencias() throws Exception {
        entrada(tokenOperario, origen, "10");
        long id = json(enviar(post("/movimientos/transferencias"), tokenOperario, Map.of(
                "productoId", producto.getId(), "depositoOrigenId", origen.getId(),
                "depositoDestinoId", destino.getId(), "cantidad", 2)).andExpect(status().isCreated()))
                .get("id").asLong();

        enviar(patch("/movimientos/" + id + "/confirmar"), tokenOperario, null).andExpect(status().isForbidden());
        enviar(patch("/movimientos/" + id + "/cancelar"), tokenOperario, null).andExpect(status().isForbidden());
        mockMvc.perform(get("/stock").header("Authorization", bearer(tokenOperario))).andExpect(status().isOk());
        mockMvc.perform(get("/movimientos")).andExpect(status().isUnauthorized());
    }

    @Test
    void salidaConStockInsuficiente_esConflictoYNoRegistraNada() throws Exception {
        entrada(tokenOperario, origen, "5");

        enviar(post("/movimientos/salidas"), tokenOperario, Map.of(
                "productoId", producto.getId(), "depositoOrigenId", origen.getId(),
                "cantidad", 6, "motivo", "venta"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));

        assertThat(cantidadEn(stock(), origen)).isEqualByComparingTo("5");
    }

    @Test
    void validacionesDeNegocio_devuelvenSusCodigos() throws Exception {
        enviar(post("/movimientos/salidas"), tokenOperario, Map.of(
                "productoId", producto.getId(), "depositoOrigenId", origen.getId(), "cantidad", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("MOTIVO_OBLIGATORIO"));

        enviar(post("/movimientos/transferencias"), tokenOperario, Map.of(
                "productoId", producto.getId(), "depositoOrigenId", origen.getId(),
                "depositoDestinoId", origen.getId(), "cantidad", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DEPOSITOS_IGUALES"));

        // El producto del test no es fraccionable (RN-44).
        enviar(post("/movimientos/entradas"), tokenOperario, Map.of(
                "productoId", producto.getId(), "depositoDestinoId", origen.getId(), "cantidad", 2.5))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CANTIDAD_NO_ENTERA"));

        enviar(post("/movimientos/entradas"), tokenOperario, Map.of(
                "productoId", producto.getId(), "depositoDestinoId", origen.getId(), "cantidad", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidad").exists());

        Deposito inactivo = depositoRepository.save(Deposito.builder().nombre("Baja " + sufijo()).activo(false).build());
        enviar(post("/movimientos/entradas"), tokenOperario, Map.of(
                "productoId", producto.getId(), "depositoDestinoId", inactivo.getId(), "cantidad", 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DEPOSITO_INACTIVO"));
    }

    @Test
    void laFechaNoLaPuedeMandarElCliente() throws Exception {
        enviar(post("/movimientos/entradas"), tokenOperario, Map.of(
                "productoId", producto.getId(), "depositoDestinoId", origen.getId(), "cantidad", 1,
                "fecha", "2020-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CAMPO_NO_PERMITIDO"));
    }

    @Test
    void respuestaDeUnMovimiento_tieneElFormatoDelContrato() throws Exception {
        enviar(post("/movimientos/entradas"), tokenOperario, Map.of(
                "productoId", producto.getId(), "depositoDestinoId", origen.getId(),
                "cantidad", 3, "motivo", "compra"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.producto.id").value(producto.getId()))
                .andExpect(jsonPath("$.producto.unidadMedida").value("un"))
                .andExpect(jsonPath("$.fecha", endsWith("Z")))
                .andExpect(jsonPath("$.usuario.id").value(operario.getId()))
                .andExpect(jsonPath("$.usuario.nombre").value("Nombre"))
                .andExpect(jsonPath("$.depositoOrigen").doesNotExist())
                .andExpect(jsonPath("$.depositoDestino.id").value(origen.getId()))
                .andExpect(jsonPath("$.estadoTransferencia").doesNotExist());
    }

    @Test
    void historial_paginaOrdenaYFiltra() throws Exception {
        entrada(tokenOperario, origen, "1");
        entrada(tokenOperario, origen, "2");
        entrada(tokenOperario, destino, "3");

        JsonNode pagina1 = json(mockMvc.perform(get("/movimientos")
                        .param("productoId", producto.getId().toString()).param("size", "2").param("page", "0")
                        .header("Authorization", bearer(tokenOperario)))
                .andExpect(status().isOk()));
        assertThat(pagina1.get("contenido")).hasSize(2);
        assertThat(pagina1.get("pagina").asInt()).isZero();
        assertThat(pagina1.get("tamanio").asInt()).isEqualTo(2);
        assertThat(pagina1.get("totalElementos").asLong()).isEqualTo(3);
        assertThat(pagina1.get("totalPaginas").asInt()).isEqualTo(2);
        // El más reciente primero: la última entrada fue la de 3 unidades.
        assertThat(pagina1.get("contenido").get(0).get("cantidad").decimalValue()).isEqualByComparingTo("3");

        mockMvc.perform(get("/movimientos")
                        .param("productoId", producto.getId().toString())
                        .param("depositoId", destino.getId().toString())
                        .header("Authorization", bearer(tokenOperario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));

        mockMvc.perform(get("/movimientos")
                        .param("productoId", producto.getId().toString())
                        .param("tipo", "SALIDA")
                        .header("Authorization", bearer(tokenOperario)))
                .andExpect(jsonPath("$.totalElementos").value(0));

        // Todos los movimientos se hicieron hoy (hora de Argentina o UTC), así que un día anterior no trae nada.
        mockMvc.perform(get("/movimientos")
                        .param("productoId", producto.getId().toString())
                        .param("desde", "2000-01-01").param("hasta", "2000-01-02")
                        .header("Authorization", bearer(tokenOperario)))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void stock_incluyeProductosActivosSinMovimientos_yDetalleDeUnProducto() throws Exception {
        JsonNode lista = json(mockMvc.perform(get("/stock").param("productoId", producto.getId().toString())
                        .header("Authorization", bearer(tokenOperario)))
                .andExpect(status().isOk()));
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).get("porDeposito")).isEmpty();
        assertThat(lista.get(0).get("producto").get("fraccionable").asBoolean()).isFalse();

        entrada(tokenOperario, origen, "7");
        mockMvc.perform(get("/stock/productos/" + producto.getId()).header("Authorization", bearer(tokenOperario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porDeposito[0].deposito.id").value(origen.getId()))
                .andExpect(jsonPath("$.porDeposito[0].deposito.activo").value(true));
        mockMvc.perform(get("/stock/productos/999999999").header("Authorization", bearer(tokenOperario)))
                .andExpect(status().isNotFound());
    }

    @Test
    void dosSalidasSimultaneas_noDejanElStockNegativo() throws Exception {
        entrada(tokenOperario, origen, "10");
        UsuarioActual actual = new UsuarioActual(operario.getId(), operario.getUsername(), Rol.OPERARIO);

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Callable<Boolean> pedido = () -> {
                salida.await();
                try {
                    movimientoService.registrarSalida(
                            new SalidaRequest(producto.getId(), origen.getId(), new BigDecimal("8"), "venta"), actual);
                    return true;
                } catch (ConflictException e) {
                    assertThat(e.getCodigo()).isEqualTo("STOCK_INSUFICIENTE");
                    return false;
                }
            };
            resultados.add(pool.submit(pedido));
        }
        salida.countDown();
        int exitosas = 0;
        for (Future<Boolean> r : resultados) {
            if (r.get()) {
                exitosas++;
            }
        }
        pool.shutdown();

        assertThat(exitosas).isEqualTo(1);
        assertThat(cantidadEn(stock(), origen)).isEqualByComparingTo("2");
    }

    // --------------------------------------------------------------- ayudas

    private static String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Usuario crearUsuario(Rol rol) {
        return usuarioRepository.save(Usuario.builder()
                .username("u-" + sufijo()).passwordHash(passwordEncoder.encode(CLAVE))
                .nombre("Nombre").apellido("Apellido").rol(rol).activo(true).build());
    }

    private String login(Usuario usuario) throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of("username", usuario.getUsername(), "password", CLAVE));
        String respuesta = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("token").asText();
    }

    private Producto crearProducto(boolean fraccionable) {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nombre("Cat " + sufijo()).build());
        Proveedor proveedor = proveedorRepository.save(Proveedor.builder().nombre("Prov " + sufijo())
                .leadTimeDias(5).build());
        return productoRepository.save(Producto.builder()
                .codigo("P-" + sufijo()).nombre("Producto " + sufijo())
                .categoria(categoria).proveedor(proveedor)
                .unidadMedida("un").precioReferencia(new BigDecimal("100.00"))
                .stockSeguridad(BigDecimal.ZERO).fraccionable(fraccionable).build());
    }

    private Deposito crearDeposito() {
        return depositoRepository.save(Deposito.builder().nombre("Dep " + sufijo()).activo(true).build());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private ResultActions enviar(MockHttpServletRequestBuilder pedido, String token, Object cuerpo) throws Exception {
        pedido.header("Authorization", bearer(token));
        if (cuerpo != null) {
            pedido.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(cuerpo));
        }
        return mockMvc.perform(pedido);
    }

    private JsonNode json(ResultActions resultado) throws Exception {
        return objectMapper.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private void entrada(String token, Deposito deposito, String cantidad) throws Exception {
        enviar(post("/movimientos/entradas"), token, Map.of(
                "productoId", producto.getId(), "depositoDestinoId", deposito.getId(),
                "cantidad", new BigDecimal(cantidad))).andExpect(status().isCreated());
    }

    private JsonNode stock() throws Exception {
        return json(mockMvc.perform(get("/stock/productos/" + producto.getId())
                        .header("Authorization", bearer(tokenOperario)))
                .andExpect(status().isOk()));
    }

    private static BigDecimal cantidadEn(JsonNode stock, Deposito deposito) {
        for (JsonNode fila : stock.get("porDeposito")) {
            if (fila.get("deposito").get("id").asLong() == deposito.getId()) {
                return fila.get("cantidad").decimalValue();
            }
        }
        return BigDecimal.ZERO;
    }
}
