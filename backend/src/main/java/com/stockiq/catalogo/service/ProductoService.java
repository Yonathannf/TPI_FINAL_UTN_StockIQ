package com.stockiq.catalogo.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.stockiq.common.exception.NotFoundException;
import com.stockiq.stock.repository.StockConsultaRepository;

import lombok.RequiredArgsConstructor;

/** RF-06, RF-08, RN-30, RN-32, RN-39, RN-42, RN-43, RN-47. */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final StockConsultaRepository stockConsulta;
    private final CatalogoMapper mapper;

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(Long categoriaId, Long proveedorId, Boolean activo, String texto) {
        String patron = (texto == null || texto.isBlank())
                ? null
                : "%" + texto.trim().toLowerCase(Locale.ROOT) + "%";
        return productoRepository.buscar(categoriaId, proveedorId, activo, patron)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return productoRepository.buscarConRelaciones(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> noExiste(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest req) {
        if (productoRepository.existsByCodigoIgnoreCase(req.codigo().trim())) {
            throw codigoDuplicado(req.codigo());
        }
        Producto producto = new Producto();
        mapper.copiar(req, producto);
        producto.setCategoria(categoriaAsignable(req.categoriaId(), null));
        producto.setProveedor(proveedorAsignable(req.proveedorId(), null));
        return mapper.toResponse(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse editar(Long id, ProductoRequest req) {
        // Se bloquea la fila: el cambio de "fraccionable" no puede cruzarse con un movimiento (RN-47, D-13)
        Producto producto = productoRepository.bloquearPorId(id).orElseThrow(() -> noExiste(id));

        if (productoRepository.existsByCodigoIgnoreCaseAndIdNot(req.codigo().trim(), id)) {
            throw codigoDuplicado(req.codigo());
        }
        if (producto.isFraccionable() && !req.fraccionable()
                && stockConsulta.productoTieneStockNoEntero(id)) {
            throw new ConflictException("STOCK_NO_ENTERO",
                    "El producto tiene stock con decimales: no puede pasar a no fraccionable hasta que sea entero.");
        }

        Categoria categoria = categoriaAsignable(req.categoriaId(), producto.getCategoria());
        Proveedor proveedor = proveedorAsignable(req.proveedorId(), producto.getProveedor());

        mapper.copiar(req, producto);
        producto.setCategoria(categoria);
        producto.setProveedor(proveedor);
        return mapper.toResponse(producto);
    }

    /** RN-39: no se desactiva con stock, stock en tránsito o transferencias pendientes. */
    @Transactional
    public ProductoResponse desactivar(Long id) {
        Producto producto = productoRepository.bloquearPorId(id).orElseThrow(() -> noExiste(id));
        if (!producto.isActivo()) {
            return mapper.toResponse(producto);
        }
        if (stockConsulta.productoTieneStock(id)) {
            throw new ConflictException("PRODUCTO_CON_STOCK",
                    "El producto todavía tiene stock en algún depósito. Registrá la salida o la transferencia antes de darlo de baja.");
        }
        if (stockConsulta.productoTieneTransferenciasPendientes(id)) {
            throw new ConflictException("PRODUCTO_CON_TRANSFERENCIAS",
                    "El producto tiene transferencias pendientes. Confirmalas o cancelalas antes de darlo de baja.");
        }
        producto.setActivo(false);
        return mapper.toResponse(producto);
    }

    @Transactional
    public ProductoResponse activar(Long id) {
        Producto producto = productoRepository.buscarConRelaciones(id).orElseThrow(() -> noExiste(id));
        producto.setActivo(true);
        return mapper.toResponse(producto);
    }

    /**
     * RN-43: una categoría inactiva no puede asignarse. Si el producto ya la tenía
     * ({@code actual}), la conserva aunque esté inactiva.
     */
    private Categoria categoriaAsignable(Long categoriaId, Categoria actual) {
        if (actual != null && Objects.equals(actual.getId(), categoriaId)) {
            return actual;
        }
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> NotFoundException.of("Categoría", categoriaId));
        if (!categoria.isActivo()) {
            throw new ConflictException("CATEGORIA_INACTIVA",
                    "La categoría '" + categoria.getNombre() + "' está inactiva y no puede asignarse a un producto.");
        }
        return categoria;
    }

    /** RN-43 y RN-42: mismo criterio que la categoría. */
    private Proveedor proveedorAsignable(Long proveedorId, Proveedor actual) {
        if (actual != null && Objects.equals(actual.getId(), proveedorId)) {
            return actual;
        }
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> NotFoundException.of("Proveedor", proveedorId));
        if (!proveedor.isActivo()) {
            throw new ConflictException("PROVEEDOR_INACTIVO",
                    "El proveedor '" + proveedor.getNombre() + "' está inactivo y no puede asignarse a un producto.");
        }
        return proveedor;
    }

    private static NotFoundException noExiste(Long id) {
        return NotFoundException.of("Producto", id);
    }

    private static ConflictException codigoDuplicado(String codigo) {
        return new ConflictException("CODIGO_DUPLICADO",
                "Ya existe un producto con el código '" + codigo.trim() + "'.");
    }
}
