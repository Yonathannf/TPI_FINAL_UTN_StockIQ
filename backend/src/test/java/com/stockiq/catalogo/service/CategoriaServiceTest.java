package com.stockiq.catalogo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import com.stockiq.catalogo.dto.CategoriaRequest;
import com.stockiq.catalogo.dto.CategoriaResponse;
import com.stockiq.catalogo.mapper.CatalogoMapper;
import com.stockiq.catalogo.model.Categoria;
import com.stockiq.catalogo.repository.CategoriaRepository;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.common.exception.ConflictException;
import com.stockiq.common.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    private static final Sort ORDEN = Sort.by("nombre");

    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private ProductoRepository productoRepository;

    private CategoriaService service;

    @BeforeEach
    void setUp() {
        service = new CategoriaService(categoriaRepository, productoRepository, new CatalogoMapper());
    }

    private static Categoria categoria(Long id) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNombre("Bebidas");
        return c;
    }

    // ---------- Alta ----------

    @Test
    void rechazaNombreDuplicadoAlCrear() {
        when(categoriaRepository.existsByNombreIgnoreCase("Bebidas")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new CategoriaRequest(" Bebidas ", null)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("NOMBRE_DUPLICADO");
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void creaUnaCategoriaActivaSinEspaciosSobrantes() {
        when(categoriaRepository.existsByNombreIgnoreCase("Lácteos")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaResponse respuesta = service.crear(new CategoriaRequest("  Lácteos ", "   "));

        assertThat(respuesta.nombre()).isEqualTo("Lácteos");
        assertThat(respuesta.descripcion()).isNull();
        assertThat(respuesta.activo()).isTrue();
    }

    // ---------- Edición ----------

    @Test
    void editaElNombreYLaDescripcion() {
        Categoria c = categoria(1L);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(c));
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdNot("Gaseosas", 1L)).thenReturn(false);

        CategoriaResponse respuesta = service.editar(1L, new CategoriaRequest(" Gaseosas ", " Con gas "));

        assertThat(respuesta.nombre()).isEqualTo("Gaseosas");
        assertThat(respuesta.descripcion()).isEqualTo("Con gas");
        assertThat(c.getNombre()).isEqualTo("Gaseosas");
    }

    @Test
    void rechazaNombreDuplicadoAlEditar() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdNot("Lácteos", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.editar(1L, new CategoriaRequest("Lácteos", null)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("NOMBRE_DUPLICADO");
    }

    @Test
    void editarUnaCategoriaInexistenteDa404() {
        when(categoriaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editar(9L, new CategoriaRequest("Lácteos", null)))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- Baja lógica y reactivación ----------

    @Test
    void desactivaUnaCategoriaSinTocarSusProductos() {
        Categoria c = categoria(1L);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(c));

        CategoriaResponse respuesta = service.desactivar(1L);

        assertThat(respuesta.activo()).isFalse();
        assertThat(c.isActivo()).isFalse();
        verify(productoRepository, never()).existsByCategoriaId(any());
    }

    @Test
    void activaUnaCategoriaInactiva() {
        Categoria c = categoria(1L);
        c.setActivo(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThat(service.activar(1L).activo()).isTrue();
        assertThat(c.isActivo()).isTrue();
    }

    // ---------- Eliminación física ----------

    @Test
    void noEliminaUnaCategoriaConProductos() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(productoRepository.existsByCategoriaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("CATEGORIA_CON_PRODUCTOS");
        verify(categoriaRepository, never()).delete(any());
    }

    @Test
    void eliminaUnaCategoriaSinProductos() {
        Categoria c = categoria(1L);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(c));
        when(productoRepository.existsByCategoriaId(1L)).thenReturn(false);

        service.eliminar(1L);

        verify(categoriaRepository).delete(c);
    }

    // ---------- Consulta ----------

    @Test
    void categoriaInexistenteDa404() {
        when(categoriaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void obtenerDevuelveLaCategoria() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));

        CategoriaResponse respuesta = service.obtener(1L);

        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.nombre()).isEqualTo("Bebidas");
    }

    @Test
    void listarSinFiltroDevuelveTodasOrdenadasPorNombre() {
        when(categoriaRepository.findAll(ORDEN)).thenReturn(List.of(categoria(1L), categoria(2L)));

        List<CategoriaResponse> respuesta = service.listar(null);

        assertThat(respuesta).extracting(CategoriaResponse::id).containsExactly(1L, 2L);
        verify(categoriaRepository, never()).findByActivo(anyBoolean(), any(Sort.class));
    }

    @Test
    void listarConFiltroConsultaPorEstado() {
        when(categoriaRepository.findByActivo(false, ORDEN)).thenReturn(List.of(categoria(3L)));

        List<CategoriaResponse> respuesta = service.listar(false);

        assertThat(respuesta).extracting(CategoriaResponse::id).containsExactly(3L);
    }
}
