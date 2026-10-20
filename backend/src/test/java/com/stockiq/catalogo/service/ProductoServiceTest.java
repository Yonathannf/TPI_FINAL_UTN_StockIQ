package com.stockiq.catalogo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stockiq.catalogo.dto.ProductoRequest;
import com.stockiq.catalogo.dto.ProductoResponse;
import com.stockiq.catalogo.mapper.CatalogoMapper;
import com.stockiq.catalogo.model.Categoria;
import com.stockiq.catalogo.model.Producto;
import com.stockiq.catalogo.model.Proveedor;
import com.stockiq.catalogo.repository.CategoriaRepository;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.catalogo.repository.ProveedorRepository;
import com.stockiq.common.exception.ConflictException;
import com.stockiq.stock.repository.StockConsultaRepository;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private StockConsultaRepository stockConsulta;

    private ProductoService service;

    @BeforeEach
    void setUp() {
        service = new ProductoService(productoRepository, categoriaRepository, proveedorRepository,
                stockConsulta, new CatalogoMapper());
    }

    private static Categoria categoria(Long id, boolean activa) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNombre("Categoría " + id);
        c.setActivo(activa);
        return c;
    }

    private static Proveedor proveedor(Long id, boolean activo) {
        Proveedor p = new Proveedor();
        p.setId(id);
        p.setNombre("Proveedor " + id);
        p.setLeadTimeDias(5);
        p.setActivo(activo);
        return p;
    }

    private static Producto producto(Long id, Categoria c, Proveedor p, boolean fraccionable) {
        Producto pr = new Producto();
        pr.setId(id);
        pr.setCodigo("P-" + id);
        pr.setNombre("Producto " + id);
        pr.setCategoria(c);
        pr.setProveedor(p);
        pr.setUnidadMedida("kg");
        pr.setPrecioReferencia(new BigDecimal("100.00"));
        pr.setStockSeguridad(BigDecimal.TEN);
        pr.setFraccionable(fraccionable);
        pr.setActivo(true);
        return pr;
    }

    private static ProductoRequest request(Long categoriaId, Long proveedorId, boolean fraccionable) {
        return new ProductoRequest("P-1", "Producto 1", categoriaId, proveedorId, "kg",
                new BigDecimal("100.00"), BigDecimal.TEN, fraccionable);
    }

    @Nested
    @DisplayName("Crear (RN-43)")
    class Crear {

        @Test
        void rechazaCodigoDuplicado() {
            when(productoRepository.existsByCodigoIgnoreCase("P-1")).thenReturn(true);

            assertThatThrownBy(() -> service.crear(request(1L, 1L, true)))
                    .isInstanceOf(ConflictException.class)
                    .extracting("codigo").isEqualTo("CODIGO_DUPLICADO");
        }

        @Test
        void rechazaCategoriaInactiva() {
            when(productoRepository.existsByCodigoIgnoreCase("P-1")).thenReturn(false);
            when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L, false)));

            assertThatThrownBy(() -> service.crear(request(1L, 1L, true)))
                    .isInstanceOf(ConflictException.class)
                    .extracting("codigo").isEqualTo("CATEGORIA_INACTIVA");
            verify(productoRepository, never()).save(any());
        }

        @Test
        void rechazaProveedorInactivo() {
            when(productoRepository.existsByCodigoIgnoreCase("P-1")).thenReturn(false);
            when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L, true)));
            when(proveedorRepository.findById(2L)).thenReturn(Optional.of(proveedor(2L, false)));

            assertThatThrownBy(() -> service.crear(request(1L, 2L, true)))
                    .isInstanceOf(ConflictException.class)
                    .extracting("codigo").isEqualTo("PROVEEDOR_INACTIVO");
        }

        @Test
        void creaConCategoriaYProveedorActivos() {
            when(productoRepository.existsByCodigoIgnoreCase("P-1")).thenReturn(false);
            when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L, true)));
            when(proveedorRepository.findById(2L)).thenReturn(Optional.of(proveedor(2L, true)));
            when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> {
                Producto p = inv.getArgument(0);
                p.setId(10L);
                return p;
            });

            ProductoResponse res = service.crear(request(1L, 2L, false));

            assertThat(res.id()).isEqualTo(10L);
            assertThat(res.categoria().id()).isEqualTo(1L);
            assertThat(res.proveedor().id()).isEqualTo(2L);
            assertThat(res.fraccionable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Editar (RN-42, RN-43, RN-47)")
    class Editar {

        @Test
        void conservaUnaCategoriaYUnProveedorQueYaTeniaAunqueEstenInactivos() {
            Producto existente = producto(5L, categoria(1L, false), proveedor(2L, false), true);
            when(productoRepository.bloquearPorId(5L)).thenReturn(Optional.of(existente));
            when(productoRepository.existsByCodigoIgnoreCaseAndIdNot("P-1", 5L)).thenReturn(false);

            ProductoResponse res = service.editar(5L, request(1L, 2L, true));

            assertThat(res.categoria().activo()).isFalse();
            assertThat(res.proveedor().activo()).isFalse();
            verify(categoriaRepository, never()).findById(anyLong());
            verify(proveedorRepository, never()).findById(anyLong());
        }

        @Test
        void noPermiteCambiarAUnaCategoriaInactiva() {
            Producto existente = producto(5L, categoria(1L, true), proveedor(2L, true), true);
            when(productoRepository.bloquearPorId(5L)).thenReturn(Optional.of(existente));
            when(productoRepository.existsByCodigoIgnoreCaseAndIdNot("P-1", 5L)).thenReturn(false);
            when(categoriaRepository.findById(3L)).thenReturn(Optional.of(categoria(3L, false)));

            assertThatThrownBy(() -> service.editar(5L, request(3L, 2L, true)))
                    .isInstanceOf(ConflictException.class)
                    .extracting("codigo").isEqualTo("CATEGORIA_INACTIVA");
            assertThat(existente.getCategoria().getId()).isEqualTo(1L);
        }

        @Test
        void noPermitePasarANoFraccionableConStockNoEntero() {
            Producto existente = producto(5L, categoria(1L, true), proveedor(2L, true), true);
            when(productoRepository.bloquearPorId(5L)).thenReturn(Optional.of(existente));
            when(productoRepository.existsByCodigoIgnoreCaseAndIdNot("P-1", 5L)).thenReturn(false);
            when(stockConsulta.productoTieneStockNoEntero(5L)).thenReturn(true);

            assertThatThrownBy(() -> service.editar(5L, request(1L, 2L, false)))
                    .isInstanceOf(ConflictException.class)
                    .extracting("codigo").isEqualTo("STOCK_NO_ENTERO");
            assertThat(existente.isFraccionable()).isTrue();
        }

        @Test
        void permitePasarANoFraccionableConStockEntero() {
            Producto existente = producto(5L, categoria(1L, true), proveedor(2L, true), true);
            when(productoRepository.bloquearPorId(5L)).thenReturn(Optional.of(existente));
            when(productoRepository.existsByCodigoIgnoreCaseAndIdNot("P-1", 5L)).thenReturn(false);
            when(stockConsulta.productoTieneStockNoEntero(5L)).thenReturn(false);

            ProductoResponse res = service.editar(5L, request(1L, 2L, false));

            assertThat(res.fraccionable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Desactivar (RN-39)")
    class Desactivar {

        @Test
        void noPermiteDesactivarConStock() {
            Producto existente = producto(5L, categoria(1L, true), proveedor(2L, true), true);
            when(productoRepository.bloquearPorId(5L)).thenReturn(Optional.of(existente));
            when(stockConsulta.productoTieneStock(5L)).thenReturn(true);

            assertThatThrownBy(() -> service.desactivar(5L))
                    .isInstanceOf(ConflictException.class)
                    .extracting("codigo").isEqualTo("PRODUCTO_CON_STOCK");
            assertThat(existente.isActivo()).isTrue();
        }

        @Test
        void noPermiteDesactivarConTransferenciasPendientes() {
            Producto existente = producto(5L, categoria(1L, true), proveedor(2L, true), true);
            when(productoRepository.bloquearPorId(5L)).thenReturn(Optional.of(existente));
            when(stockConsulta.productoTieneStock(5L)).thenReturn(false);
            when(stockConsulta.productoTieneTransferenciasPendientes(5L)).thenReturn(true);

            assertThatThrownBy(() -> service.desactivar(5L))
                    .isInstanceOf(ConflictException.class)
                    .extracting("codigo").isEqualTo("PRODUCTO_CON_TRANSFERENCIAS");
        }

        @Test
        void desactivaSinStockNiTransferencias() {
            Producto existente = producto(5L, categoria(1L, true), proveedor(2L, true), true);
            when(productoRepository.bloquearPorId(5L)).thenReturn(Optional.of(existente));
            when(stockConsulta.productoTieneStock(5L)).thenReturn(false);
            when(stockConsulta.productoTieneTransferenciasPendientes(5L)).thenReturn(false);

            assertThat(service.desactivar(5L).activo()).isFalse();
        }
    }

    @Test
    void elFiltroDeTextoSeEnviaEnMinusculasYConComodines() {
        when(productoRepository.buscar(null, null, true, "%cable%")).thenReturn(List.of());

        service.listar(null, null, true, "  CABLE ");

        verify(productoRepository).buscar(null, null, true, "%cable%");
    }
}
