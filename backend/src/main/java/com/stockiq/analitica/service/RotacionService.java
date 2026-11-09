package com.stockiq.analitica.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockiq.analitica.dto.CategoriaRef;
import com.stockiq.analitica.dto.RotacionCategoriaResponse;
import com.stockiq.analitica.dto.RotacionCategoriasResponse;
import com.stockiq.analitica.dto.RotacionProductoResponse;
import com.stockiq.analitica.dto.RotacionProductosResponse;
import com.stockiq.analitica.repository.AnaliticaConsultaRepository;
import com.stockiq.catalogo.model.Producto;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.parametro.service.ParametroService;

import lombok.RequiredArgsConstructor;

/**
 * RF-20; RN-25, RN-26 y RN-36. Rotación = salidas del período / stock promedio, donde el stock promedio es
 * el promedio entre el stock al inicio y al final del período, sobre todos los depósitos (RN-36).
 */
@Service
@RequiredArgsConstructor
public class RotacionService {

    private static final BigDecimal DOS = new BigDecimal("2");

    private final ProductoRepository productoRepository;
    private final AnaliticaConsultaRepository consultas;
    private final ParametroService parametroService;
    private final Clock clock;

    /** Por producto activo, de mayor a menor rotación. */
    @Transactional(readOnly = true)
    public RotacionProductosResponse porProducto(LocalDate desde, LocalDate hasta) {
        Periodo periodo = periodo(desde, hasta);
        List<RotacionProductoResponse> filas = calcular(periodo).stream()
                .map(Fila::respuesta)
                .sorted(Comparator.comparing(RotacionProductoResponse::rotacion).reversed()
                        .thenComparing(r -> r.producto().nombre(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new RotacionProductosResponse(periodo.desde(), periodo.hasta(), filas);
    }

    /**
     * RN-26: por categoría, sumando las salidas y el stock promedio de sus productos activos. Incluye las
     * categorías inactivas que todavía tienen productos activos (RN-43).
     */
    @Transactional(readOnly = true)
    public RotacionCategoriasResponse porCategoria(LocalDate desde, LocalDate hasta) {
        Periodo periodo = periodo(desde, hasta);
        Map<Long, List<Fila>> porCategoria = new LinkedHashMap<>();
        for (Fila fila : calcular(periodo)) {
            porCategoria.computeIfAbsent(fila.producto().getCategoria().getId(), k -> new ArrayList<>()).add(fila);
        }
        List<RotacionCategoriaResponse> categorias = porCategoria.values().stream()
                .map(RotacionService::sumar)
                .sorted(Comparator.comparing(RotacionCategoriaResponse::rotacion).reversed()
                        .thenComparing(r -> r.categoria().nombre(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new RotacionCategoriasResponse(periodo.desde(), periodo.hasta(), categorias);
    }

    Periodo periodo(LocalDate desde, LocalDate hasta) {
        return Periodo.de(desde, hasta, parametroService.configuracion().periodoAnalisisDias(), clock);
    }

    private List<Fila> calcular(Periodo periodo) {
        Map<Long, BigDecimal> salidas = consultas.salidasPorProducto(periodo.inicio(), periodo.fin());
        Map<Long, BigDecimal> inicial = consultas.stockTotalAl(periodo.inicio());
        Map<Long, BigDecimal> fin = consultas.stockTotalAl(periodo.fin());
        return productoRepository.buscar(null, null, true, null).stream()
                .map(p -> new Fila(p,
                        salidas.getOrDefault(p.getId(), BigDecimal.ZERO),
                        inicial.getOrDefault(p.getId(), BigDecimal.ZERO),
                        fin.getOrDefault(p.getId(), BigDecimal.ZERO)))
                .toList();
    }

    /** RN-25: si el stock promedio es 0, la rotación es 0. */
    static BigDecimal rotacion(BigDecimal salidas, BigDecimal stockPromedio) {
        return Refs.dos(Refs.dividir(salidas, stockPromedio));
    }

    private static RotacionCategoriaResponse sumar(List<Fila> filas) {
        Producto primero = filas.get(0).producto();
        CategoriaRef categoria = Refs.categoria(primero);
        BigDecimal salidas = filas.stream().map(Fila::salidas).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = filas.stream().map(Fila::stockPromedio).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RotacionCategoriaResponse(categoria, filas.size(), Refs.dos(salidas), Refs.dos(promedio),
                rotacion(salidas, promedio));
    }

    private record Fila(Producto producto, BigDecimal salidas, BigDecimal stockInicial, BigDecimal stockFinal) {

        BigDecimal stockPromedio() {
            return stockInicial.add(stockFinal).divide(DOS);
        }

        RotacionProductoResponse respuesta() {
            return new RotacionProductoResponse(Refs.producto(producto), Refs.categoria(producto),
                    Refs.dos(salidas), Refs.dos(stockInicial), Refs.dos(stockFinal), Refs.dos(stockPromedio()),
                    rotacion(salidas, stockPromedio()));
        }
    }
}
