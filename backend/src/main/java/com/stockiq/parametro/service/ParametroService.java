package com.stockiq.parametro.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockiq.common.exception.BadRequestException;
import com.stockiq.common.exception.NotFoundException;
import com.stockiq.parametro.dto.ParametroResponse;
import com.stockiq.parametro.model.Parametro;
import com.stockiq.parametro.repository.ParametroRepository;

import lombok.RequiredArgsConstructor;

/** RF-22, RN-37: lectura y edición de los parámetros de la capa analítica. */
@Service
@RequiredArgsConstructor
public class ParametroService {

    public static final String VENTANA_CONSUMO_DIAS = "ventana_consumo_dias";
    public static final String DIAS_COBERTURA = "dias_cobertura";
    public static final String PERIODO_ANALISIS_DIAS = "periodo_analisis_dias";
    public static final String CORTE_ABC_A = "corte_abc_a";
    public static final String CORTE_ABC_B = "corte_abc_b";

    /** Tope razonable para las ventanas en días (10 años). */
    static final int MAX_DIAS = 3650;
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ParametroRepository parametroRepository;

    @Transactional(readOnly = true)
    public List<ParametroResponse> listar() {
        return parametroRepository.findAll(Sort.by("clave")).stream().map(ParametroService::toResponse).toList();
    }

    /** RN-37: valida el valor según el tipo de parámetro y lo guarda normalizado. */
    @Transactional
    public ParametroResponse actualizar(String clave, String valor) {
        Parametro parametro = parametroRepository.findById(clave)
                .orElseThrow(() -> new NotFoundException("No existe el parámetro '" + clave + "'."));
        String texto = valor.trim();

        String normalizado = switch (clave) {
            case VENTANA_CONSUMO_DIAS, DIAS_COBERTURA, PERIODO_ANALISIS_DIAS -> String.valueOf(validarDias(texto));
            case CORTE_ABC_A, CORTE_ABC_B -> validarCorte(clave, texto).toPlainString();
            default -> throw new BadRequestException("PARAMETRO_NO_EDITABLE",
                    "El parámetro '" + clave + "' no se puede modificar.");
        };

        parametro.setValor(normalizado);
        return toResponse(parametro);
    }

    /** Parámetros convertidos a su tipo. Si alguno falta o es inválido en la base, usa el valor por defecto. */
    @Transactional(readOnly = true)
    public ParametrosAnalitica configuracion() {
        Map<String, String> valores = parametroRepository.findAll().stream()
                .collect(Collectors.toMap(Parametro::getClave, Parametro::getValor));
        ParametrosAnalitica d = ParametrosAnalitica.POR_DEFECTO;
        return new ParametrosAnalitica(
                leer(valores, VENTANA_CONSUMO_DIAS, Integer::valueOf, d.ventanaConsumoDias()),
                leer(valores, DIAS_COBERTURA, Integer::valueOf, d.diasCobertura()),
                leer(valores, PERIODO_ANALISIS_DIAS, Integer::valueOf, d.periodoAnalisisDias()),
                leer(valores, CORTE_ABC_A, BigDecimal::new, d.corteAbcA()),
                leer(valores, CORTE_ABC_B, BigDecimal::new, d.corteAbcB()));
    }

    // ---------------------------------------------------------------- validaciones (RN-37)

    private static int validarDias(String texto) {
        try {
            int dias = Integer.parseInt(texto);
            if (dias >= 1 && dias <= MAX_DIAS) {
                return dias;
            }
        } catch (NumberFormatException e) {
            // se informa abajo
        }
        throw new BadRequestException("PARAMETRO_INVALIDO",
                "El valor tiene que ser un número entero de días entre 1 y " + MAX_DIAS + ".");
    }

    private BigDecimal validarCorte(String clave, String texto) {
        BigDecimal corte;
        try {
            corte = new BigDecimal(texto.replace(',', '.')).stripTrailingZeros();
        } catch (NumberFormatException e) {
            throw new BadRequestException("PARAMETRO_INVALIDO", "El corte tiene que ser un número entre 0 y 100.");
        }
        if (corte.signum() <= 0 || corte.compareTo(CIEN) >= 0 || corte.scale() > 2) {
            throw new BadRequestException("PARAMETRO_INVALIDO",
                    "El corte tiene que ser un porcentaje mayor a 0 y menor a 100, con hasta dos decimales.");
        }
        // corte_abc_a tiene que ser menor que corte_abc_b
        ParametrosAnalitica actual = configuracion();
        boolean esA = CORTE_ABC_A.equals(clave);
        BigDecimal a = esA ? corte : actual.corteAbcA();
        BigDecimal b = esA ? actual.corteAbcB() : corte;
        if (a.compareTo(b) >= 0) {
            throw new BadRequestException("CORTES_ABC_INVALIDOS",
                    "El corte de la clase A (" + a.toPlainString() + "%) tiene que ser menor que el de la clase B ("
                            + b.toPlainString() + "%).");
        }
        return corte.scale() < 0 ? corte.setScale(0) : corte;
    }

    private static <T> T leer(Map<String, String> valores, String clave, Function<String, T> conversor,
                              T porDefecto) {
        try {
            String valor = valores.get(clave);
            return valor == null ? porDefecto : conversor.apply(valor.trim());
        } catch (RuntimeException e) {
            return porDefecto;
        }
    }

    private static ParametroResponse toResponse(Parametro p) {
        return new ParametroResponse(p.getClave(), p.getValor(), p.getDescripcion());
    }
}
