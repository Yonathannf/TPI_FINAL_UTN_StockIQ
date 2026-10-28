package com.stockiq.movimiento.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import com.stockiq.deposito.model.Deposito;
import com.stockiq.movimiento.dto.MovimientoResponse;
import com.stockiq.movimiento.dto.MovimientoResponse.DepositoRef;
import com.stockiq.movimiento.dto.MovimientoResponse.ProductoRef;
import com.stockiq.movimiento.dto.MovimientoResponse.UsuarioRef;
import com.stockiq.movimiento.model.Movimiento;
import com.stockiq.usuario.model.Usuario;

@Component
public class MovimientoMapper {

    public MovimientoResponse toResponse(Movimiento m) {
        return new MovimientoResponse(
                m.getId(),
                m.getTipo(),
                new ProductoRef(m.getProducto().getId(), m.getProducto().getCodigo(),
                        m.getProducto().getNombre(), m.getProducto().getUnidadMedida()),
                m.getCantidad(),
                utc(m.getFecha()),
                m.getMotivo(),
                usuario(m.getUsuario()),
                deposito(m.getDepositoOrigen()),
                deposito(m.getDepositoDestino()),
                m.getEstadoTransferencia(),
                utc(m.getFechaResolucion()),
                usuario(m.getUsuarioResolucion()));
    }

    /** Las fechas se guardan en UTC (D-16); con OffsetDateTime Jackson agrega la "Z" al serializar. */
    private static OffsetDateTime utc(LocalDateTime fecha) {
        return fecha == null ? null : fecha.atOffset(ZoneOffset.UTC);
    }

    private static UsuarioRef usuario(Usuario u) {
        return u == null ? null : new UsuarioRef(u.getId(), u.getNombre(), u.getApellido());
    }

    private static DepositoRef deposito(Deposito d) {
        return d == null ? null : new DepositoRef(d.getId(), d.getNombre());
    }
}
