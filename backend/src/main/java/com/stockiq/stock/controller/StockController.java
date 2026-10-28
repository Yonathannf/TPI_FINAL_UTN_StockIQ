package com.stockiq.stock.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockiq.stock.dto.StockProductoResponse;
import com.stockiq.stock.dto.StockTransitoResponse;
import com.stockiq.stock.service.StockService;

import lombok.RequiredArgsConstructor;

/** RF-16, RF-17. Consulta de stock para todos los roles autenticados (RN-34). */
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService service;

    @GetMapping
    public List<StockProductoResponse> listar(@RequestParam(required = false) Long productoId,
                                              @RequestParam(required = false) Long depositoId) {
        return service.listar(productoId, depositoId);
    }

    @GetMapping("/productos/{id}")
    public StockProductoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/transito")
    public List<StockTransitoResponse> transito() {
        return service.transito();
    }
}
