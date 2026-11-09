package com.stockiq.parametro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Parámetro configurable de la capa analítica (D-08). El valor se guarda como texto y se valida en el Service. */
@Entity
@Table(name = "parametro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parametro {

    @Id
    @Column(length = 50)
    private String clave;

    @Column(nullable = false, length = 50)
    private String valor;

    @Column(length = 255)
    private String descripcion;
}
