package com.stockiq.analitica.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockiq.analitica.dto.AbcResponse;
import com.stockiq.analitica.dto.ReposicionResponse;
import com.stockiq.analitica.dto.RotacionCategoriasResponse;
import com.stockiq.analitica.dto.RotacionProductosResponse;
import com.stockiq.analitica.service.AbcService;
import com.stockiq.analitica.service.ReposicionService;
import com.stockiq.analitica.service.RotacionService;

import lombok.RequiredArgsConstructor;

/** RF-18 a RF-21. Solo ENCARGADO: el OPERARIO no ve la capa analítica (RN-35; la regla está en SecurityConfig). */
@RestController
@RequestMapping("/analitica")
@RequiredArgsConstructor
public class AnaliticaController {

    private final ReposicionService reposicionService;
    private final RotacionService rotacionService;
    private final AbcService abcService;

    @GetMapping("/reposicion")
    public List<ReposicionResponse> reposicion(@RequestParam(defaultValue = "false") boolean soloAlertas) {
        return reposicionService.calcular(soloAlertas);
    }

    @GetMapping("/rotacion/productos")
    public RotacionProductosResponse rotacionPorProducto(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return rotacionService.porProducto(desde, hasta);
    }

    @GetMapping("/rotacion/categorias")
    public RotacionCategoriasResponse rotacionPorCategoria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return rotacionService.porCategoria(desde, hasta);
    }

    @GetMapping("/abc")
    public AbcResponse abc() {
        return abcService.calcular();
    }
}
