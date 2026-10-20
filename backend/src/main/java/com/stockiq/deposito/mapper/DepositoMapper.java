package com.stockiq.deposito.mapper;

import org.springframework.stereotype.Component;

import com.stockiq.deposito.dto.DepositoRequest;
import com.stockiq.deposito.dto.DepositoResponse;
import com.stockiq.deposito.model.Deposito;

@Component
public class DepositoMapper {

    public DepositoResponse toResponse(Deposito d) {
        return new DepositoResponse(d.getId(), d.getNombre(), d.getUbicacion(), d.isActivo());
    }

    public void copiar(DepositoRequest req, Deposito d) {
        d.setNombre(req.nombre().trim());
        d.setUbicacion(req.ubicacion() == null || req.ubicacion().isBlank() ? null : req.ubicacion().trim());
    }
}
