package com.stockiq.dashboard.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockiq.dashboard.dto.Agrupacion;
import com.stockiq.dashboard.dto.DashboardAnaliticaResponse;
import com.stockiq.dashboard.dto.DashboardStockResponse;
import com.stockiq.dashboard.dto.EvolucionResponse;
import com.stockiq.dashboard.service.DashboardService;

import lombok.RequiredArgsConstructor;

/**
 * RF-23 a RF-25. /dashboard/stock es para todos los roles; /dashboard/analitica y /dashboard/evolucion, solo
 * para ENCARGADO (las reglas están en SecurityConfig).
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/analitica")
    public DashboardAnaliticaResponse analitica() {
        return service.analitica();
    }

    @GetMapping("/stock")
    public DashboardStockResponse stock() {
        return service.stock();
    }

    @GetMapping("/evolucion")
    public EvolucionResponse evolucion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Agrupacion agrupacion) {
        return service.evolucion(desde, hasta, agrupacion);
    }
}
