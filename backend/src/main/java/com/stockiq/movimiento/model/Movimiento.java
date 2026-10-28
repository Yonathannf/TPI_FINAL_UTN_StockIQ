package com.stockiq.movimiento.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.stockiq.catalogo.model.Producto;
import com.stockiq.deposito.model.Deposito;
import com.stockiq.usuario.model.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * Movimiento de stock. No se edita ni se borra (RN-07): lo único que cambia es la resolución de una
 * transferencia pendiente. Las fechas se guardan en UTC (D-16).
 */
@Entity
@Table(name = "movimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // La columna es un ENUM de MySQL (V1); el columnDefinition deja explícito el tipo para ddl-auto=validate.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('ENTRADA','SALIDA','TRANSFERENCIA')")
    private TipoMovimiento tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    /** RN-41: la asigna el servidor, en UTC. */
    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 255)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposito_origen_id")
    private Deposito depositoOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposito_destino_id")
    private Deposito depositoDestino;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_transferencia",
            columnDefinition = "ENUM('PENDIENTE','CONFIRMADA','CANCELADA')")
    private EstadoTransferencia estadoTransferencia;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_resolucion_id")
    private Usuario usuarioResolucion;
}
