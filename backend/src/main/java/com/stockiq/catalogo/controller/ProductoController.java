package com.stockiq.catalogo.controller;

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

import com.stockiq.catalogo.dto.ProductoRequest;
import com.stockiq.catalogo.dto.ProductoResponse;
import com.stockiq.catalogo.service.ProductoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** RF-06, RF-08. Lectura para todos los roles (el operario elige productos al registrar movimientos); escritura solo ENCARGADO (SecurityConfig). */
@RestController
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    @GetMapping
    public List<ProductoResponse> listar(@RequestParam(required = false) Long categoriaId,
                                         @RequestParam(required = false) Long proveedorId,
                                         @RequestParam(required = false) Boolean activo,
                                         @RequestParam(required = false) String texto) {
        return service.listar(categoriaId, proveedorId, activo, texto);
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest req) {
        ProductoResponse creado = service.crear(req);
        return ResponseEntity.created(URI.create("/productos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public ProductoResponse editar(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return service.editar(id, req);
    }

    @PatchMapping("/{id}/desactivar")
    public ProductoResponse desactivar(@PathVariable Long id) {
        return service.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public ProductoResponse activar(@PathVariable Long id) {
        return service.activar(id);
    }
}
