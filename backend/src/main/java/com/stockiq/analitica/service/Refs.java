package com.stockiq.analitica.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.stockiq.analitica.dto.CategoriaRef;
import com.stockiq.analitica.dto.ProductoRef;
import com.stockiq.analitica.dto.ProveedorRef;
import com.stockiq.catalogo.model.Producto;

/** Armado de referencias y redondeo común a los indicadores. */
final class Refs {

    /** Escala de los cálculos intermedios; los resultados se redondean a 2 decimales. */
    static final int ESCALA_CALCULO = 6;
    static final BigDecimal CIEN = new BigDecimal("100");

    private Refs() {
    }

    static ProductoRef producto(Producto p) {
        return new ProductoRef(p.getId(), p.getCodigo(), p.getNombre(), p.getUnidadMedida(), p.isFraccionable());
    }

    static CategoriaRef categoria(Producto p) {
        return new CategoriaRef(p.getCategoria().getId(), p.getCategoria().getNombre(), p.getCategoria().isActivo());
    }

    static ProveedorRef proveedor(Producto p) {
        return new ProveedorRef(p.getProveedor().getId(), p.getProveedor().getNombre(),
                p.getProveedor().getLeadTimeDias(), p.getProveedor().isActivo());
    }

    static BigDecimal dos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal dividir(BigDecimal dividendo, BigDecimal divisor) {
        return divisor.signum() == 0 ? BigDecimal.ZERO : dividendo.divide(divisor, ESCALA_CALCULO, RoundingMode.HALF_UP);
    }
}
