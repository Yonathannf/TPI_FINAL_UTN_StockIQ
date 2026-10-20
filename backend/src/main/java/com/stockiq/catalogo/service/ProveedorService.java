package com.stockiq.catalogo.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockiq.catalogo.dto.ProveedorRequest;
import com.stockiq.catalogo.dto.ProveedorResponse;
import com.stockiq.catalogo.mapper.CatalogoMapper;
import com.stockiq.catalogo.model.Proveedor;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.catalogo.repository.ProveedorRepository;
import com.stockiq.common.exception.ConflictException;
import com.stockiq.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

/** RF-05, RF-08, RN-31, RN-42, RN-46. */
@Service
@RequiredArgsConstructor
public class ProveedorService {

    private static final Sort ORDEN = Sort.by("nombre");

    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final CatalogoMapper mapper;

    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar(Boolean activo) {
        List<Proveedor> proveedores = activo == null
                ? proveedorRepository.findAll(ORDEN)
                : proveedorRepository.findByActivo(activo, ORDEN);
        return proveedores.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {
        return mapper.toResponse(buscar(id));
    }

    @Transactional
    public ProveedorResponse crear(ProveedorRequest req) {
        if (proveedorRepository.existsByNombreIgnoreCase(req.nombre().trim())) {
            throw nombreDuplicado(req.nombre());
        }
        Proveedor proveedor = new Proveedor();
        mapper.copiar(req, proveedor);
        return mapper.toResponse(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorResponse editar(Long id, ProveedorRequest req) {
        Proveedor proveedor = buscar(id);
        if (proveedorRepository.existsByNombreIgnoreCaseAndIdNot(req.nombre().trim(), id)) {
            throw nombreDuplicado(req.nombre());
        }
        mapper.copiar(req, proveedor);
        return mapper.toResponse(proveedor);
    }

    /** RN-42: los productos que lo tienen como principal lo conservan; no hay baja en cascada. */
    @Transactional
    public ProveedorResponse desactivar(Long id) {
        Proveedor proveedor = buscar(id);
        proveedor.setActivo(false);
        return mapper.toResponse(proveedor);
    }

    @Transactional
    public ProveedorResponse activar(Long id) {
        Proveedor proveedor = buscar(id);
        proveedor.setActivo(true);
        return mapper.toResponse(proveedor);
    }

    /** RN-31, RN-46: solo se elimina físicamente si no tiene productos asociados. */
    @Transactional
    public void eliminar(Long id) {
        Proveedor proveedor = buscar(id);
        if (productoRepository.existsByProveedorId(id)) {
            throw new ConflictException("PROVEEDOR_CON_PRODUCTOS",
                    "El proveedor tiene productos asociados: no se puede eliminar, solo desactivar.");
        }
        proveedorRepository.delete(proveedor);
    }

    private Proveedor buscar(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Proveedor", id));
    }

    private static ConflictException nombreDuplicado(String nombre) {
        return new ConflictException("NOMBRE_DUPLICADO",
                "Ya existe un proveedor con el nombre '" + nombre.trim() + "'.");
    }
}
