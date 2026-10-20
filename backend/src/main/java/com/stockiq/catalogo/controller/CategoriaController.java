package com.stockiq.catalogo.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockiq.catalogo.dto.CategoriaRequest;
import com.stockiq.catalogo.dto.CategoriaResponse;
import com.stockiq.catalogo.service.CategoriaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** RF-04. Lectura para todos los roles y escritura solo ENCARGADO (la regla está en SecurityConfig). */
@RestController
@RequestMapping("/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService service;

    @GetMapping
    public List<CategoriaResponse> listar(@RequestParam(required = false) Boolean activo) {
        return service.listar(activo);
    }

    @GetMapping("/{id}")
    public CategoriaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest req) {
        CategoriaResponse creada = service.crear(req);
        return ResponseEntity.created(URI.create("/categorias/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    public CategoriaResponse editar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest req) {
        return service.editar(id, req);
    }

    @PatchMapping("/{id}/desactivar")
    public CategoriaResponse desactivar(@PathVariable Long id) {
        return service.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public CategoriaResponse activar(@PathVariable Long id) {
        return service.activar(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
