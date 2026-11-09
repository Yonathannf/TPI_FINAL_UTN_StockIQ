package com.stockiq.parametro.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockiq.parametro.dto.ParametroResponse;
import com.stockiq.parametro.dto.ParametroUpdateRequest;
import com.stockiq.parametro.service.ParametroService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** RF-22. Solo ENCARGADO (la regla está en SecurityConfig). */
@RestController
@RequestMapping("/parametros")
@RequiredArgsConstructor
public class ParametroController {

    private final ParametroService service;

    @GetMapping
    public List<ParametroResponse> listar() {
        return service.listar();
    }

    @PutMapping("/{clave}")
    public ParametroResponse actualizar(@PathVariable String clave, @Valid @RequestBody ParametroUpdateRequest req) {
        return service.actualizar(clave, req.valor());
    }
}
