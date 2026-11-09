package com.stockiq.analitica.service;

import java.math.BigDecimal;

import com.stockiq.catalogo.model.Categoria;
import com.stockiq.catalogo.model.Producto;
import com.stockiq.catalogo.model.Proveedor;

/** Productos de prueba para los tests de la capa analítica. */
final class Datos {

    static final Categoria BEBIDAS = Categoria.builder().id(1L).nombre("Bebidas").activo(true).build();
    static final Categoria SNACKS = Categoria.builder().id(2L).nombre("Snacks").activo(false).build();

    private Datos() {
    }

    static Producto producto(long id, String precio, int leadTime, String seguridad, boolean fraccionable,
                             Categoria categoria) {
        Proveedor proveedor = Proveedor.builder().id(100 + id).nombre("Prov " + id).leadTimeDias(leadTime)
                .activo(true).build();
        return Producto.builder().id(id).codigo("P-" + id).nombre("Producto " + id)
                .categoria(categoria).proveedor(proveedor).unidadMedida("un")
                .precioReferencia(new BigDecimal(precio)).stockSeguridad(new BigDecimal(seguridad))
                .fraccionable(fraccionable).activo(true).build();
    }

    static Producto producto(long id, String precio) {
        return producto(id, precio, 5, "0", false, BEBIDAS);
    }
}
