package com.stockiq.deposito.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockiq.deposito.dto.DepositoRequest;
import com.stockiq.deposito.dto.DepositoResponse;
import com.stockiq.deposito.service.DepositoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** RF-07, RF-08. Lectura para todos los roles y escritura solo ENCARGADO (la regla está en SecurityConfig). */
@RestController
@RequestMapping("/depositos")
@RequiredArgsConstructor
public class DepositoController {

    private final DepositoService service;

    @GetMapping
    public List<DepositoResponse> listar(@RequestParam(required = false) Boolean activo) {
        return service.listar(activo);
    }

    @GetMapping("/{id}")
    public DepositoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<DepositoResponse> crear(@Valid @RequestBody DepositoRequest req) {
        DepositoResponse creado = service.crear(req);
        return ResponseEntity.created(URI.create("/depositos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public DepositoResponse editar(@PathVariable Long id, @Valid @RequestBody DepositoRequest req) {
        return service.editar(id, req);
    }

    @PatchMapping("/{id}/desactivar")
    public DepositoResponse desactivar(@PathVariable Long id) {
        return service.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public DepositoResponse activar(@PathVariable Long id) {
        return service.activar(id);
    }
}
