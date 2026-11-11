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

import com.stockiq.catalogo.dto.ProveedorRequest;
import com.stockiq.catalogo.dto.ProveedorResponse;
import com.stockiq.catalogo.mapper.CatalogoMapper;
import com.stockiq.catalogo.model.Proveedor;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.catalogo.repository.ProveedorRepository;
import com.stockiq.common.exception.ConflictException;
import com.stockiq.common.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    private static final Sort ORDEN = Sort.by("nombre");

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

    // ---------- Eliminación y baja lógica ----------

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
    void eliminaUnProveedorSinProductos() {
        Proveedor p = proveedor(1L);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(p));
        when(productoRepository.existsByProveedorId(1L)).thenReturn(false);

        service.eliminar(1L);

        verify(proveedorRepository).delete(p);
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
    void activaUnProveedorInactivo() {
        Proveedor p = proveedor(1L);
        p.setActivo(false);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThat(service.activar(1L).activo()).isTrue();
        assertThat(p.isActivo()).isTrue();
    }

    // ---------- Alta ----------

    @Test
    void creaUnProveedorActivoConLosTextosLimpios() {
        when(proveedorRepository.existsByNombreIgnoreCase("Distribuidora Norte")).thenReturn(false);
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        ProveedorResponse respuesta = service.crear(
                new ProveedorRequest("  Distribuidora Norte ", " Ana ", "  ", "ana@norte.com", 5));

        assertThat(respuesta.nombre()).isEqualTo("Distribuidora Norte");
        assertThat(respuesta.contacto()).isEqualTo("Ana");
        assertThat(respuesta.telefono()).isNull();
        assertThat(respuesta.email()).isEqualTo("ana@norte.com");
        assertThat(respuesta.leadTimeDias()).isEqualTo(5);
        assertThat(respuesta.activo()).isTrue();
    }

    @Test
    void rechazaNombreDuplicadoAlCrear() {
        when(proveedorRepository.existsByNombreIgnoreCase("Distribuidora Sur")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new ProveedorRequest("Distribuidora Sur", null, null, null, 3)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("NOMBRE_DUPLICADO");
        verify(proveedorRepository, never()).save(any());
    }

    // ---------- Edición ----------

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

    @Test
    void rechazaNombreDuplicadoAlEditar() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L)));
        when(proveedorRepository.existsByNombreIgnoreCaseAndIdNot("Otra Distribuidora", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.editar(1L, new ProveedorRequest("Otra Distribuidora", null, null, null, 4)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("NOMBRE_DUPLICADO");
    }

    @Test
    void editarUnProveedorInexistenteDa404() {
        when(proveedorRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editar(9L, new ProveedorRequest("Nuevo", null, null, null, 4)))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- Consulta ----------

    @Test
    void obtenerDevuelveElProveedor() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L)));

        ProveedorResponse respuesta = service.obtener(1L);

        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.leadTimeDias()).isEqualTo(7);
    }

    @Test
    void obtenerUnProveedorInexistenteDa404() {
        when(proveedorRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void listarSinFiltroDevuelveTodosOrdenadosPorNombre() {
        when(proveedorRepository.findAll(ORDEN)).thenReturn(List.of(proveedor(1L), proveedor(2L)));

        List<ProveedorResponse> respuesta = service.listar(null);

        assertThat(respuesta).extracting(ProveedorResponse::id).containsExactly(1L, 2L);
        verify(proveedorRepository, never()).findByActivo(anyBoolean(), any(Sort.class));
    }

    @Test
    void listarConFiltroConsultaPorEstado() {
        when(proveedorRepository.findByActivo(true, ORDEN)).thenReturn(List.of(proveedor(1L)));

        assertThat(service.listar(true)).hasSize(1);
    }
}
