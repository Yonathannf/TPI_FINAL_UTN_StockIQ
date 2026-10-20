package com.stockiq.catalogo.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.stockiq.catalogo.dto.ProveedorRequest;
import com.stockiq.catalogo.mapper.CatalogoMapper;
import com.stockiq.catalogo.model.Proveedor;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.catalogo.repository.ProveedorRepository;
import com.stockiq.common.exception.ConflictException;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private ProductoRepository productoRepository;

    private ProveedorService service;

    @BeforeEach
    void setUp() {
        service = new ProveedorService(proveedorRepository, productoRepository, new CatalogoMapper());
    }

    private static Proveedor proveedor(Long id) {
        Proveedor p = new Proveedor();
        p.setId(id);
        p.setNombre("Distribuidora Sur");
        p.setLeadTimeDias(7);
        p.setActivo(true);
        return p;
    }

    @Test
    void noEliminaUnProveedorConProductos() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L)));
        when(productoRepository.existsByProveedorId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("PROVEEDOR_CON_PRODUCTOS");
        verify(proveedorRepository, never()).delete(any());
    }

    @Test
    void desactivarNoConsultaLosProductos() {
        Proveedor p = proveedor(1L);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(p));

        service.desactivar(1L);

        assertThat(p.isActivo()).isFalse();
        verify(productoRepository, never()).existsByProveedorId(any());
    }

    @Test
    void guardaLosCamposOpcionalesVaciosComoNull() {
        Proveedor p = proveedor(1L);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(p));
        when(proveedorRepository.existsByNombreIgnoreCaseAndIdNot("Distribuidora Sur", 1L)).thenReturn(false);

        service.editar(1L, new ProveedorRequest("Distribuidora Sur", "  ", "", null, 10));

        assertThat(p.getContacto()).isNull();
        assertThat(p.getTelefono()).isNull();
        assertThat(p.getLeadTimeDias()).isEqualTo(10);
    }
}
