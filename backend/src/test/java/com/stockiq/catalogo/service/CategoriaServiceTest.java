package com.stockiq.catalogo.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stockiq.catalogo.dto.CategoriaRequest;
import com.stockiq.catalogo.mapper.CatalogoMapper;
import com.stockiq.catalogo.model.Categoria;
import com.stockiq.catalogo.repository.CategoriaRepository;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.common.exception.ConflictException;
import com.stockiq.common.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

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

    @Test
    void rechazaNombreDuplicadoAlCrear() {
        when(categoriaRepository.existsByNombreIgnoreCase("Bebidas")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new CategoriaRequest(" Bebidas ", null)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("NOMBRE_DUPLICADO");
        verify(categoriaRepository, never()).save(any());
    }

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

    @Test
    void categoriaInexistenteDa404() {
        when(categoriaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(9L)).isInstanceOf(NotFoundException.class);
    }
}
