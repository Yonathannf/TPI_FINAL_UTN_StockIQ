package com.stockiq.catalogo.mapper;

import org.springframework.stereotype.Component;

import com.stockiq.catalogo.dto.CategoriaRequest;
import com.stockiq.catalogo.dto.CategoriaResponse;
import com.stockiq.catalogo.dto.ProductoRequest;
import com.stockiq.catalogo.dto.ProductoResponse;
import com.stockiq.catalogo.dto.ProveedorRequest;
import com.stockiq.catalogo.dto.ProveedorResponse;
import com.stockiq.catalogo.dto.ReferenciaResponse;
import com.stockiq.catalogo.model.Categoria;
import com.stockiq.catalogo.model.Producto;
import com.stockiq.catalogo.model.Proveedor;

/** Conversión entre entidades y DTOs del catálogo. Los textos se guardan sin espacios sobrantes. */
@Component
public class CatalogoMapper {

    // ---------- Categoría ----------
    public CategoriaResponse toResponse(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion(), c.isActivo());
    }

    public void copiar(CategoriaRequest req, Categoria c) {
        c.setNombre(req.nombre().trim());
        c.setDescripcion(limpiar(req.descripcion()));
    }

    // ---------- Proveedor ----------
    public ProveedorResponse toResponse(Proveedor p) {
        return new ProveedorResponse(p.getId(), p.getNombre(), p.getContacto(), p.getTelefono(),
                p.getEmail(), p.getLeadTimeDias(), p.isActivo());
    }

    public void copiar(ProveedorRequest req, Proveedor p) {
        p.setNombre(req.nombre().trim());
        p.setContacto(limpiar(req.contacto()));
        p.setTelefono(limpiar(req.telefono()));
        p.setEmail(limpiar(req.email()));
        p.setLeadTimeDias(req.leadTimeDias());
    }

    // ---------- Producto ----------
    public ProductoResponse toResponse(Producto p) {
        Categoria c = p.getCategoria();
        Proveedor pr = p.getProveedor();
        return new ProductoResponse(
                p.getId(),
                p.getCodigo(),
                p.getNombre(),
                new ReferenciaResponse(c.getId(), c.getNombre(), c.isActivo()),
                new ReferenciaResponse(pr.getId(), pr.getNombre(), pr.isActivo()),
                p.getUnidadMedida(),
                p.getPrecioReferencia(),
                p.getStockSeguridad(),
                p.isFraccionable(),
                p.isActivo());
    }

    /** Copia los campos simples. La categoría y el proveedor los asigna el Service tras validarlos. */
    public void copiar(ProductoRequest req, Producto p) {
        p.setCodigo(req.codigo().trim());
        p.setNombre(req.nombre().trim());
        p.setUnidadMedida(req.unidadMedida().trim());
        p.setPrecioReferencia(req.precioReferencia());
        p.setStockSeguridad(req.stockSeguridad());
        p.setFraccionable(req.fraccionable());
    }

    /** Texto opcional: null si viene vacío o solo con espacios. */
    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}
