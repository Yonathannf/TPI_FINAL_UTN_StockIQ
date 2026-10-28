package com.stockiq.movimiento.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.stockiq.movimiento.model.EstadoTransferencia;
import com.stockiq.movimiento.model.TipoMovimiento;

/**
 * Movimiento con sus referencias anidadas. Las fechas salen en UTC con "Z" (OffsetDateTime); el frontend
 * las muestra en hora de Argentina. {@code depositoOrigen} es null en las entradas y {@code depositoDestino}
 * en las salidas; los datos de resolución son null salvo en transferencias resueltas (RN-14).
 */
public record MovimientoResponse(
        Long id,
        TipoMovimiento tipo,
        ProductoRef producto,
        BigDecimal cantidad,
        OffsetDateTime fecha,
        String motivo,
        UsuarioRef usuario,
        DepositoRef depositoOrigen,
        DepositoRef depositoDestino,
        EstadoTransferencia estadoTransferencia,
        OffsetDateTime fechaResolucion,
        UsuarioRef usuarioResolucion
) {

    public record ProductoRef(Long id, String codigo, String nombre, String unidadMedida) {
    }

    public record UsuarioRef(Long id, String nombre, String apellido) {
    }

    public record DepositoRef(Long id, String nombre) {
    }
}
