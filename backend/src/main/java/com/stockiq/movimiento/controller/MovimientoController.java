package com.stockiq.movimiento.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockiq.common.dto.PaginaResponse;
import com.stockiq.movimiento.dto.EntradaRequest;
import com.stockiq.movimiento.dto.MovimientoResponse;
import com.stockiq.movimiento.dto.SalidaRequest;
import com.stockiq.movimiento.dto.TransferenciaRequest;
import com.stockiq.movimiento.model.TipoMovimiento;
import com.stockiq.movimiento.service.MovimientoService;
import com.stockiq.security.UsuarioActual;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * RF-09 a RF-15, RF-24. Registrar y consultar: cualquier usuario autenticado (RN-34); confirmar y cancelar
 * transferencias: solo ENCARGADO (la regla está en SecurityConfig). El usuario responsable sale del token.
 */
@RestController
@RequestMapping("/movimientos")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService service;

    @PostMapping("/entradas")
    public ResponseEntity<MovimientoResponse> entrada(@Valid @RequestBody EntradaRequest req,
                                                      @AuthenticationPrincipal UsuarioActual actual) {
        return creado(service.registrarEntrada(req, actual));
    }

    @PostMapping("/salidas")
    public ResponseEntity<MovimientoResponse> salida(@Valid @RequestBody SalidaRequest req,
                                                     @AuthenticationPrincipal UsuarioActual actual) {
        return creado(service.registrarSalida(req, actual));
    }

    @PostMapping("/transferencias")
    public ResponseEntity<MovimientoResponse> transferencia(@Valid @RequestBody TransferenciaRequest req,
                                                            @AuthenticationPrincipal UsuarioActual actual) {
        return creado(service.registrarTransferencia(req, actual));
    }

    @PatchMapping("/{id}/confirmar")
    public MovimientoResponse confirmar(@PathVariable Long id, @AuthenticationPrincipal UsuarioActual actual) {
        return service.confirmar(id, actual);
    }

    @PatchMapping("/{id}/cancelar")
    public MovimientoResponse cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioActual actual) {
        return service.cancelar(id, actual);
    }

    @GetMapping
    public PaginaResponse<MovimientoResponse> historial(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long depositoId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "page", defaultValue = "0") int pagina,
            @RequestParam(name = "size", defaultValue = "20") int tamanio) {
        return service.historial(productoId, depositoId, tipo, usuarioId, desde, hasta, pagina, tamanio);
    }

    @GetMapping("/transferencias/pendientes")
    public List<MovimientoResponse> pendientes(@RequestParam(required = false) Long depositoDestinoId) {
        return service.pendientes(depositoDestinoId);
    }

    private static ResponseEntity<MovimientoResponse> creado(MovimientoResponse m) {
        return ResponseEntity.created(URI.create("/movimientos/" + m.id())).body(m);
    }
}
