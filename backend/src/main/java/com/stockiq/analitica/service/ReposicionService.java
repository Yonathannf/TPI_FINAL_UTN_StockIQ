package com.stockiq.analitica.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockiq.analitica.dto.ReposicionResponse;
import com.stockiq.analitica.repository.AnaliticaConsultaRepository;
import com.stockiq.catalogo.model.Producto;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.common.util.Fechas;
import com.stockiq.parametro.service.ParametroService;
import com.stockiq.parametro.service.ParametrosAnalitica;
import com.stockiq.stock.repository.StockConsultaRepository;

import lombok.RequiredArgsConstructor;

/** RF-18, RF-19; RN-19 a RN-24 y RN-29. Se calcula a nivel global, sobre los productos activos. */
@Service
@RequiredArgsConstructor
public class ReposicionService {

    private final ProductoRepository productoRepository;
    private final AnaliticaConsultaRepository consultas;
    private final StockConsultaRepository stockConsulta;
    private final ParametroService parametroService;
    private final Clock clock;

    /** Todos los productos activos, o solo los que están en alerta. Primero los que están en alerta. */
    @Transactional(readOnly = true)
    public List<ReposicionResponse> calcular(boolean soloAlertas) {
        ParametrosAnalitica p = parametroService.configuracion();
        LocalDateTime ahora = Fechas.ahoraUtc(clock);
        BigDecimal ventana = BigDecimal.valueOf(p.ventanaConsumoDias());

        Map<Long, BigDecimal> salidas = consultas.salidasPorProducto(ahora.minusDays(p.ventanaConsumoDias()), ahora);
        Map<Long, BigDecimal> disponible = consultas.stockDisponiblePorProducto();
        Map<Long, BigDecimal> transito = stockConsulta.enTransitoPorProducto(null);

        return productoRepository.buscar(null, null, true, null).stream()
                .map(prod -> calcular(prod,
                        Refs.dividir(salidas.getOrDefault(prod.getId(), BigDecimal.ZERO), ventana),
                        disponible.getOrDefault(prod.getId(), BigDecimal.ZERO),
                        transito.getOrDefault(prod.getId(), BigDecimal.ZERO),
                        p.diasCobertura()))
                .filter(r -> !soloAlertas || r.alerta())
                .sorted(Comparator.comparing(ReposicionResponse::alerta).reversed()
                        .thenComparing(r -> r.producto().nombre(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Cálculo de un producto. Visible en el paquete para testearlo con números concretos.
     *
     * @param consumoDiario RN-19 (0 si no tuvo salidas en la ventana, RN-21)
     */
    ReposicionResponse calcular(Producto producto, BigDecimal consumoDiario, BigDecimal stockDisponible,
                                BigDecimal enTransito, int diasCobertura) {
        BigDecimal leadTime = BigDecimal.valueOf(producto.getProveedor().getLeadTimeDias());
        BigDecimal seguridad = producto.getStockSeguridad();

        // RN-20 (RN-22: con lead time 0 queda igual al stock de seguridad)
        BigDecimal puntoReposicion = consumoDiario.multiply(leadTime).add(seguridad);
        // RN-23
        boolean alerta = stockDisponible.compareTo(puntoReposicion) <= 0;
        // RN-24: nunca menor a 0
        BigDecimal sugerida = puntoReposicion
                .add(consumoDiario.multiply(BigDecimal.valueOf(diasCobertura)))
                .subtract(stockDisponible)
                .subtract(enTransito)
                .max(BigDecimal.ZERO);
        // Se pide siempre hacia arriba: entero si el producto no admite fracciones (RN-44)
        sugerida = producto.isFraccionable()
                ? sugerida.setScale(2, RoundingMode.CEILING)
                : sugerida.setScale(0, RoundingMode.CEILING);

        return new ReposicionResponse(
                Refs.producto(producto),
                Refs.proveedor(producto),
                Refs.dos(consumoDiario),
                Refs.dos(seguridad),
                Refs.dos(puntoReposicion),
                Refs.dos(stockDisponible),
                Refs.dos(enTransito),
                alerta,
                sugerida);
    }
}
