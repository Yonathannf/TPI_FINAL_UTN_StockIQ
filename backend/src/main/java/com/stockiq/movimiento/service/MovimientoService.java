package com.stockiq.movimiento.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockiq.catalogo.model.Producto;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.common.dto.PaginaResponse;
import com.stockiq.common.exception.BadRequestException;
import com.stockiq.common.exception.ConflictException;
import com.stockiq.common.exception.NotFoundException;
import com.stockiq.deposito.model.Deposito;
import com.stockiq.deposito.repository.DepositoRepository;
import com.stockiq.movimiento.dto.EntradaRequest;
import com.stockiq.movimiento.dto.MovimientoResponse;
import com.stockiq.movimiento.dto.SalidaRequest;
import com.stockiq.movimiento.dto.TransferenciaRequest;
import com.stockiq.movimiento.mapper.MovimientoMapper;
import com.stockiq.movimiento.model.EstadoTransferencia;
import com.stockiq.movimiento.model.Movimiento;
import com.stockiq.movimiento.model.TipoMovimiento;
import com.stockiq.movimiento.repository.MovimientoRepository;
import com.stockiq.movimiento.repository.MovimientoSpecs;
import com.stockiq.security.UsuarioActual;
import com.stockiq.stock.repository.StockConsultaRepository;
import com.stockiq.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * RF-09 a RF-15, RF-24; RN-01 a RN-14, RN-41, RN-44. Las salidas y transferencias bloquean la fila del
 * producto antes de validar el stock, y confirmar o cancelar bloquea la transferencia (D-13).
 */
@Service
@RequiredArgsConstructor
public class MovimientoService {

    /** Los días de los filtros del historial son en hora de Argentina; en la base se guarda UTC. */
    static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final int TAMANIO_DEFECTO = 20;
    private static final int TAMANIO_MAXIMO = 100;

    private final MovimientoRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final DepositoRepository depositoRepository;
    private final UsuarioRepository usuarioRepository;
    private final StockConsultaRepository stockConsulta;
    private final MovimientoMapper mapper;

    /** RN-03. */
    @Transactional
    public MovimientoResponse registrarEntrada(EntradaRequest req, UsuarioActual actual) {
        Producto producto = productoValido(req.productoId(), req.cantidad());
        Deposito destino = depositoValido(req.depositoDestinoId());
        return guardar(TipoMovimiento.ENTRADA, producto, req.cantidad(), motivo(req.motivo()), actual,
                null, destino, null);
    }

    /** RN-04, RN-06, RN-09. */
    @Transactional
    public MovimientoResponse registrarSalida(SalidaRequest req, UsuarioActual actual) {
        String motivo = motivo(req.motivo());
        if (motivo == null) {
            throw new BadRequestException("MOTIVO_OBLIGATORIO", "El motivo es obligatorio en las salidas.");
        }
        Producto producto = productoValido(req.productoId(), req.cantidad());
        Deposito origen = depositoValido(req.depositoOrigenId());
        validarStock(producto, origen, req.cantidad());
        return guardar(TipoMovimiento.SALIDA, producto, req.cantidad(), motivo, actual,
                origen, null, null);
    }

    /** RN-05, RN-06, RN-10: queda PENDIENTE y descuenta del origen. */
    @Transactional
    public MovimientoResponse registrarTransferencia(TransferenciaRequest req, UsuarioActual actual) {
        if (req.depositoOrigenId().equals(req.depositoDestinoId())) {
            throw new BadRequestException("DEPOSITOS_IGUALES",
                    "El depósito de origen y el de destino tienen que ser distintos.");
        }
        Producto producto = productoValido(req.productoId(), req.cantidad());
        // Se bloquean en orden de id para que dos transferencias cruzadas no se traben entre sí.
        Deposito origen;
        Deposito destino;
        if (req.depositoOrigenId() < req.depositoDestinoId()) {
            origen = depositoValido(req.depositoOrigenId());
            destino = depositoValido(req.depositoDestinoId());
        } else {
            destino = depositoValido(req.depositoDestinoId());
            origen = depositoValido(req.depositoOrigenId());
        }
        validarStock(producto, origen, req.cantidad());
        return guardar(TipoMovimiento.TRANSFERENCIA, producto, req.cantidad(), motivo(req.motivo()), actual,
                origen, destino, EstadoTransferencia.PENDIENTE);
    }

    /** RN-11: suma al destino. */
    @Transactional
    public MovimientoResponse confirmar(Long id, UsuarioActual actual) {
        return resolver(id, actual, EstadoTransferencia.CONFIRMADA);
    }

    /** RN-12: devuelve al origen. */
    @Transactional
    public MovimientoResponse cancelar(Long id, UsuarioActual actual) {
        return resolver(id, actual, EstadoTransferencia.CANCELADA);
    }

    /** RF-15: historial, del más reciente al más antiguo. {@code desde} y {@code hasta} son días de Argentina, inclusivos. */
    @Transactional(readOnly = true)
    public PaginaResponse<MovimientoResponse> historial(Long productoId, Long depositoId, TipoMovimiento tipo,
                                                        Long usuarioId, LocalDate desde, LocalDate hasta,
                                                        int pagina, int tamanio) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new BadRequestException("RANGO_FECHAS_INVALIDO", "La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
        Specification<Movimiento> filtros = Specification
                .where(MovimientoSpecs.producto(productoId))
                .and(MovimientoSpecs.deposito(depositoId))
                .and(MovimientoSpecs.tipo(tipo))
                .and(MovimientoSpecs.usuario(usuarioId))
                .and(MovimientoSpecs.desde(desde == null ? null : inicioDelDiaEnUtc(desde)))
                .and(MovimientoSpecs.antesDe(hasta == null ? null : inicioDelDiaEnUtc(hasta.plusDays(1))));
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), tamanioValido(tamanio),
                Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by(Sort.Direction.DESC, "id")));
        return PaginaResponse.de(movimientoRepository.findAll(filtros, pageable).map(mapper::toResponse));
    }

    /** RF-24: transferencias pendientes, de la más antigua a la más reciente. */
    @Transactional(readOnly = true)
    public List<MovimientoResponse> pendientes(Long depositoDestinoId) {
        Specification<Movimiento> filtros = Specification
                .where(MovimientoSpecs.transferenciasPendientes())
                .and(MovimientoSpecs.destino(depositoDestinoId));
        Sort orden = Sort.by(Sort.Direction.ASC, "fecha").and(Sort.by(Sort.Direction.ASC, "id"));
        return movimientoRepository.findAll(filtros, orden).stream().map(mapper::toResponse).toList();
    }

    // ---------------------------------------------------------------- internos

    private MovimientoResponse resolver(Long id, UsuarioActual actual, EstadoTransferencia nuevoEstado) {
        Movimiento m = movimientoRepository.bloquearPorId(id)
                .orElseThrow(() -> NotFoundException.of("Movimiento", id));
        // RN-13: una transferencia resuelta no cambia de estado.
        if (m.getTipo() != TipoMovimiento.TRANSFERENCIA || m.getEstadoTransferencia() != EstadoTransferencia.PENDIENTE) {
            throw new ConflictException("TRANSFERENCIA_NO_PENDIENTE",
                    "Solo se pueden confirmar o cancelar transferencias pendientes.");
        }
        m.setEstadoTransferencia(nuevoEstado);
        m.setFechaResolucion(ahoraUtc());
        m.setUsuarioResolucion(usuarioRepository.getReferenceById(actual.id()));
        return mapper.toResponse(m);
    }

    /** Bloquea el producto (D-13) y valida RN-08 y RN-44. */
    private Producto productoValido(Long productoId, BigDecimal cantidad) {
        Producto producto = productoRepository.bloquearPorId(productoId)
                .orElseThrow(() -> NotFoundException.of("Producto", productoId));
        if (!producto.isActivo()) {
            throw new ConflictException("PRODUCTO_INACTIVO", "El producto está inactivo y no admite movimientos.");
        }
        if (!producto.isFraccionable() && cantidad.stripTrailingZeros().scale() > 0) {
            throw new BadRequestException("CANTIDAD_NO_ENTERA",
                    "El producto no admite cantidades fraccionarias: ingresá un número entero.");
        }
        return producto;
    }

    /** Bloquea el depósito (así no se da de baja mientras se mueve stock, RN-38) y valida RN-08. */
    private Deposito depositoValido(Long depositoId) {
        Deposito deposito = depositoRepository.bloquearPorId(depositoId)
                .orElseThrow(() -> NotFoundException.of("Depósito", depositoId));
        if (!deposito.isActivo()) {
            throw new ConflictException("DEPOSITO_INACTIVO",
                    "El depósito '" + deposito.getNombre() + "' está inactivo y no admite movimientos.");
        }
        return deposito;
    }

    /** RN-06: el origen no puede quedar con stock negativo. */
    private void validarStock(Producto producto, Deposito origen, BigDecimal cantidad) {
        BigDecimal disponible = stockConsulta.stockEn(producto.getId(), origen.getId());
        if (cantidad.compareTo(disponible) > 0) {
            throw new ConflictException("STOCK_INSUFICIENTE",
                    "Stock insuficiente en '" + origen.getNombre() + "': hay " + disponible.stripTrailingZeros()
                            .toPlainString() + " " + producto.getUnidadMedida() + " y se piden "
                            + cantidad.stripTrailingZeros().toPlainString() + ".");
        }
    }

    private MovimientoResponse guardar(TipoMovimiento tipo, Producto producto, BigDecimal cantidad, String motivo,
                                       UsuarioActual actual, Deposito origen, Deposito destino,
                                       EstadoTransferencia estado) {
        Movimiento m = Movimiento.builder()
                .tipo(tipo)
                .producto(producto)
                .cantidad(cantidad)
                .fecha(ahoraUtc())
                .motivo(motivo)
                .usuario(usuarioRepository.getReferenceById(actual.id()))
                .depositoOrigen(origen)
                .depositoDestino(destino)
                .estadoTransferencia(estado)
                .build();
        return mapper.toResponse(movimientoRepository.save(m));
    }

    private static String motivo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /** RN-41: la fecha la asigna el servidor, en UTC y sin fracciones (la columna es DATETIME). */
    private static LocalDateTime ahoraUtc() {
        return LocalDateTime.now(ZoneOffset.UTC).withNano(0);
    }

    static LocalDateTime inicioDelDiaEnUtc(LocalDate dia) {
        return dia.atStartOfDay(ZONA_ARGENTINA).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private static int tamanioValido(int tamanio) {
        if (tamanio < 1) {
            return TAMANIO_DEFECTO;
        }
        return Math.min(tamanio, TAMANIO_MAXIMO);
    }
}
