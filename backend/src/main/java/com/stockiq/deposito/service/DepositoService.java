package com.stockiq.deposito.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockiq.common.exception.ConflictException;
import com.stockiq.common.exception.NotFoundException;
import com.stockiq.deposito.dto.DepositoRequest;
import com.stockiq.deposito.dto.DepositoResponse;
import com.stockiq.deposito.mapper.DepositoMapper;
import com.stockiq.deposito.model.Deposito;
import com.stockiq.deposito.repository.DepositoRepository;
import com.stockiq.stock.repository.StockConsultaRepository;

import lombok.RequiredArgsConstructor;

/** RF-07, RF-08, RN-38. */
@Service
@RequiredArgsConstructor
public class DepositoService {

    private static final Sort ORDEN = Sort.by("nombre");

    private final DepositoRepository depositoRepository;
    private final StockConsultaRepository stockConsulta;
    private final DepositoMapper mapper;

    @Transactional(readOnly = true)
    public List<DepositoResponse> listar(Boolean activo) {
        List<Deposito> depositos = activo == null
                ? depositoRepository.findAll(ORDEN)
                : depositoRepository.findByActivo(activo, ORDEN);
        return depositos.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DepositoResponse obtener(Long id) {
        return mapper.toResponse(buscar(id));
    }

    @Transactional
    public DepositoResponse crear(DepositoRequest req) {
        if (depositoRepository.existsByNombreIgnoreCase(req.nombre().trim())) {
            throw nombreDuplicado(req.nombre());
        }
        Deposito deposito = new Deposito();
        mapper.copiar(req, deposito);
        return mapper.toResponse(depositoRepository.save(deposito));
    }

    @Transactional
    public DepositoResponse editar(Long id, DepositoRequest req) {
        Deposito deposito = buscar(id);
        if (depositoRepository.existsByNombreIgnoreCaseAndIdNot(req.nombre().trim(), id)) {
            throw nombreDuplicado(req.nombre());
        }
        mapper.copiar(req, deposito);
        return mapper.toResponse(deposito);
    }

    /** RN-38: no se desactiva con stock ni con transferencias pendientes (origen o destino). */
    @Transactional
    public DepositoResponse desactivar(Long id) {
        Deposito deposito = depositoRepository.bloquearPorId(id)
                .orElseThrow(() -> noExiste(id));
        if (!deposito.isActivo()) {
            return mapper.toResponse(deposito);
        }
        if (stockConsulta.depositoTieneStock(id)) {
            throw new ConflictException("DEPOSITO_CON_STOCK",
                    "El depósito todavía tiene stock. Transferilo o registrá la salida antes de darlo de baja.");
        }
        if (stockConsulta.depositoTieneTransferenciasPendientes(id)) {
            throw new ConflictException("DEPOSITO_CON_TRANSFERENCIAS",
                    "El depósito tiene transferencias pendientes. Confirmalas o cancelalas antes de darlo de baja.");
        }
        deposito.setActivo(false);
        return mapper.toResponse(deposito);
    }

    @Transactional
    public DepositoResponse activar(Long id) {
        Deposito deposito = buscar(id);
        deposito.setActivo(true);
        return mapper.toResponse(deposito);
    }

    private Deposito buscar(Long id) {
        return depositoRepository.findById(id).orElseThrow(() -> noExiste(id));
    }

    private static NotFoundException noExiste(Long id) {
        return NotFoundException.of("Depósito", id);
    }

    private static ConflictException nombreDuplicado(String nombre) {
        return new ConflictException("NOMBRE_DUPLICADO",
                "Ya existe un depósito con el nombre '" + nombre.trim() + "'.");
    }
}
