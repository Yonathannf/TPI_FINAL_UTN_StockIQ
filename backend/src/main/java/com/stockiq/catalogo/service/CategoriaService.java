package com.stockiq.catalogo.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockiq.catalogo.dto.CategoriaRequest;
import com.stockiq.catalogo.dto.CategoriaResponse;
import com.stockiq.catalogo.mapper.CatalogoMapper;
import com.stockiq.catalogo.model.Categoria;
import com.stockiq.catalogo.repository.CategoriaRepository;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.common.exception.ConflictException;
import com.stockiq.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

/** RF-04, RF-08, RN-45, RN-46. */
@Service
@RequiredArgsConstructor
public class CategoriaService {

    private static final Sort ORDEN = Sort.by("nombre");

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final CatalogoMapper mapper;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(Boolean activo) {
        List<Categoria> categorias = activo == null
                ? categoriaRepository.findAll(ORDEN)
                : categoriaRepository.findByActivo(activo, ORDEN);
        return categorias.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return mapper.toResponse(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest req) {
        if (categoriaRepository.existsByNombreIgnoreCase(req.nombre().trim())) {
            throw nombreDuplicado(req.nombre());
        }
        Categoria categoria = new Categoria();
        mapper.copiar(req, categoria);
        return mapper.toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse editar(Long id, CategoriaRequest req) {
        Categoria categoria = buscar(id);
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(req.nombre().trim(), id)) {
            throw nombreDuplicado(req.nombre());
        }
        mapper.copiar(req, categoria);
        return mapper.toResponse(categoria);
    }

    /** RN-45: una categoría con productos puede desactivarse; los productos la conservan (RN-43). */
    @Transactional
    public CategoriaResponse desactivar(Long id) {
        Categoria categoria = buscar(id);
        categoria.setActivo(false);
        return mapper.toResponse(categoria);
    }

    @Transactional
    public CategoriaResponse activar(Long id) {
        Categoria categoria = buscar(id);
        categoria.setActivo(true);
        return mapper.toResponse(categoria);
    }

    /** RN-45, RN-46: solo se elimina físicamente si no tiene productos asociados. */
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        if (productoRepository.existsByCategoriaId(id)) {
            throw new ConflictException("CATEGORIA_CON_PRODUCTOS",
                    "La categoría tiene productos asociados: no se puede eliminar, solo desactivar.");
        }
        categoriaRepository.delete(categoria);
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Categoría", id));
    }

    private static ConflictException nombreDuplicado(String nombre) {
        return new ConflictException("NOMBRE_DUPLICADO",
                "Ya existe una categoría con el nombre '" + nombre.trim() + "'.");
    }
}
