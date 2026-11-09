package com.stockiq.analitica.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockiq.analitica.dto.AbcProductoResponse;
import com.stockiq.analitica.dto.AbcResponse;
import com.stockiq.analitica.dto.AbcResumenResponse;
import com.stockiq.analitica.dto.ClaseAbc;
import com.stockiq.analitica.repository.AnaliticaConsultaRepository;
import com.stockiq.catalogo.model.Producto;
import com.stockiq.catalogo.repository.ProductoRepository;
import com.stockiq.parametro.service.ParametroService;
import com.stockiq.parametro.service.ParametrosAnalitica;

import lombok.RequiredArgsConstructor;

/**
 * RF-21; RN-27 a RN-29 y D-12. Valor de consumo = salidas del período × precio de referencia. La clase se
 * asigna según el porcentaje acumulado PREVIO al producto, así el que cruza un corte queda en la clase
 * superior y el de mayor valor siempre es A.
 */
@Service
@RequiredArgsConstructor
public class AbcService {

    private final ProductoRepository productoRepository;
    private final AnaliticaConsultaRepository consultas;
    private final ParametroService parametroService;
    private final Clock clock;

    /** Clasificación del período de análisis (los últimos {@code periodo_analisis_dias}, hasta hoy). */
    @Transactional(readOnly = true)
    public AbcResponse calcular() {
        ParametrosAnalitica p = parametroService.configuracion();
        Periodo periodo = Periodo.de(null, null, p.periodoAnalisisDias(), clock);
        Map<Long, BigDecimal> salidas = consultas.salidasPorProducto(periodo.inicio(), periodo.fin());
        List<Producto> productos = productoRepository.buscar(null, null, true, null);
        return clasificar(productos, salidas, p, periodo);
    }

    /** Clasificación pura, sin acceso a la base. Visible en el paquete para testear los casos de RN-28. */
    AbcResponse clasificar(List<Producto> productos, Map<Long, BigDecimal> salidas, ParametrosAnalitica p,
                           Periodo periodo) {
        List<Item> items = productos.stream()
                .map(prod -> {
                    BigDecimal cantidad = salidas.getOrDefault(prod.getId(), BigDecimal.ZERO);
                    return new Item(prod, cantidad, cantidad.multiply(prod.getPrecioReferencia()));
                })
                // RN-27: de mayor a menor valor; ante igual valor, por id
                .sorted(Comparator.comparing(Item::valor).reversed().thenComparing(i -> i.producto().getId()))
                .toList();

        BigDecimal total = items.stream().map(Item::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<AbcProductoResponse> filas = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        for (Item item : items) {
            BigDecimal porcentajePrevio = porcentaje(acumulado, total);
            ClaseAbc clase = clase(item.valor(), porcentajePrevio, p);
            acumulado = acumulado.add(item.valor());
            filas.add(new AbcProductoResponse(
                    Refs.producto(item.producto()),
                    Refs.categoria(item.producto()),
                    Refs.dos(item.cantidad()),
                    Refs.dos(item.producto().getPrecioReferencia()),
                    Refs.dos(item.valor()),
                    Refs.dos(porcentaje(item.valor(), total)),
                    Refs.dos(porcentaje(acumulado, total)),
                    clase));
        }
        return new AbcResponse(periodo.desde(), periodo.hasta(), Refs.dos(total), filas, resumen(filas, total));
    }

    /** RN-27, RN-28: sin consumo es C; si no, A o B según el acumulado previo y los cortes. */
    static ClaseAbc clase(BigDecimal valor, BigDecimal porcentajePrevio, ParametrosAnalitica p) {
        if (valor.signum() == 0) {
            return ClaseAbc.C;
        }
        if (porcentajePrevio.compareTo(p.corteAbcA()) < 0) {
            return ClaseAbc.A;
        }
        if (porcentajePrevio.compareTo(p.corteAbcB()) < 0) {
            return ClaseAbc.B;
        }
        return ClaseAbc.C;
    }

    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        return Refs.dividir(parte.multiply(Refs.CIEN), total);
    }

    private static List<AbcResumenResponse> resumen(List<AbcProductoResponse> filas, BigDecimal total) {
        return Arrays.stream(ClaseAbc.values())
                .map(clase -> {
                    List<AbcProductoResponse> deLaClase = filas.stream().filter(f -> f.clase() == clase).toList();
                    BigDecimal valor = deLaClase.stream()
                            .map(AbcProductoResponse::valorConsumo)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new AbcResumenResponse(clase, deLaClase.size(), Refs.dos(valor),
                            Refs.dos(porcentaje(valor, total)));
                })
                .toList();
    }

    private record Item(Producto producto, BigDecimal cantidad, BigDecimal valor) {
    }
}
