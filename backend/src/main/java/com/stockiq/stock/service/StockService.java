package com.stockiq.stock.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockiq.catalogo.model.Producto;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.common.exception.NotFoundException;
import com.stockiq.stock.dto.DepositoStockResponse;
import com.stockiq.stock.dto.ProductoStockResponse;
import com.stockiq.stock.dto.ProductoTransitoResponse;
import com.stockiq.stock.dto.StockPorDepositoResponse;
import com.stockiq.stock.dto.StockProductoResponse;
import com.stockiq.stock.dto.StockTransitoResponse;
import com.stockiq.stock.repository.StockConsultaRepository;
import com.stockiq.stock.repository.StockConsultaRepository.StockDepositoFila;

import lombok.RequiredArgsConstructor;

/** RF-16, RF-17; RN-15 a RN-18. El stock no se guarda: se calcula a partir de los movimientos. */
@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductoRepository productoRepository;
    private final StockConsultaRepository stockConsulta;

    /**
     * Una fila por producto activo (aunque no tenga stock), ordenada por nombre. Con {@code productoId}
     * devuelve solo ese producto; con {@code depositoId}, {@code porDeposito} incluye solo ese depósito
     * (el total disponible y el tránsito siguen siendo los del producto completo).
     */
    @Transactional(readOnly = true)
    public List<StockProductoResponse> listar(Long productoId, Long depositoId) {
        List<Producto> productos = productoId == null
                ? productoRepository.buscar(null, null, true, null)
                : List.of(buscarProducto(productoId));
        Map<Long, List<StockDepositoFila>> stock = stockConsulta.stockPorDeposito(productoId).stream()
                .collect(Collectors.groupingBy(StockDepositoFila::productoId));
        Map<Long, BigDecimal> transito = stockConsulta.enTransitoPorProducto(productoId);
        return productos.stream()
                .map(p -> armar(p, stock.getOrDefault(p.getId(), List.of()),
                        transito.getOrDefault(p.getId(), BigDecimal.ZERO), depositoId))
                .toList();
    }

    /** Stock de un producto, esté activo o no. */
    @Transactional(readOnly = true)
    public StockProductoResponse obtener(Long productoId) {
        Producto producto = buscarProducto(productoId);
        return armar(producto, stockConsulta.stockPorDeposito(productoId),
                stockConsulta.enTransitoPorProducto(productoId).getOrDefault(productoId, BigDecimal.ZERO), null);
    }

    /** RN-17: productos con transferencias pendientes y cuánto hay en tránsito. */
    @Transactional(readOnly = true)
    public List<StockTransitoResponse> transito() {
        Map<Long, BigDecimal> transito = stockConsulta.enTransitoPorProducto(null);
        return productoRepository.findAllById(transito.keySet()).stream()
                .sorted(Comparator.comparing(Producto::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(p -> new StockTransitoResponse(
                        new ProductoTransitoResponse(p.getId(), p.getCodigo(), p.getNombre(), p.getUnidadMedida()),
                        transito.get(p.getId())))
                .toList();
    }

    private StockProductoResponse armar(Producto p, List<StockDepositoFila> filas, BigDecimal enTransito,
                                        Long depositoId) {
        // RN-18: el total disponible suma solo los depósitos activos, sin el tránsito.
        BigDecimal total = filas.stream()
                .filter(StockDepositoFila::depositoActivo)
                .map(StockDepositoFila::stock)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<StockPorDepositoResponse> porDeposito = filas.stream()
                .filter(f -> depositoId == null || f.depositoId().equals(depositoId))
                .map(f -> new StockPorDepositoResponse(
                        new DepositoStockResponse(f.depositoId(), f.depositoNombre(), f.depositoActivo()),
                        f.stock()))
                .toList();
        return new StockProductoResponse(
                new ProductoStockResponse(p.getId(), p.getCodigo(), p.getNombre(), p.getUnidadMedida(),
                        p.isFraccionable()),
                porDeposito, total, enTransito);
    }

    private Producto buscarProducto(Long id) {
        return productoRepository.findById(id).orElseThrow(() -> NotFoundException.of("Producto", id));
    }
}
