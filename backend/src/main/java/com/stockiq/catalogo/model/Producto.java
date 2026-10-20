package com.stockiq.catalogo.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    /** RN-30: todo producto tiene una categoría. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    /** RN-30: todo producto tiene un proveedor principal (D-03). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(name = "unidad_medida", nullable = false, length = 20)
    private String unidadMedida;

    @Column(name = "precio_referencia", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioReferencia;

    @Column(name = "stock_seguridad", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal stockSeguridad = BigDecimal.ZERO;

    /** RN-44, RN-47. Agregada en la migración V2 (D-18). */
    @Column(nullable = false)
    @Builder.Default
    private boolean fraccionable = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
