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

import com.stockiq.catalogo.dto.ProveedorRequest;
import com.stockiq.catalogo.dto.ProveedorResponse;
import com.stockiq.catalogo.service.ProveedorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** RF-05. Solo ENCARGADO (la regla está en SecurityConfig). */
@RestController
@RequestMapping("/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService service;

    @GetMapping
    public List<ProveedorResponse> listar(@RequestParam(required = false) Boolean activo) {
        return service.listar(activo);
    }

    @GetMapping("/{id}")
    public ProveedorResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest req) {
        ProveedorResponse creado = service.crear(req);
        return ResponseEntity.created(URI.create("/proveedores/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public ProveedorResponse editar(@PathVariable Long id, @Valid @RequestBody ProveedorRequest req) {
        return service.editar(id, req);
    }

    @PatchMapping("/{id}/desactivar")
    public ProveedorResponse desactivar(@PathVariable Long id) {
        return service.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public ProveedorResponse activar(@PathVariable Long id) {
        return service.activar(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
