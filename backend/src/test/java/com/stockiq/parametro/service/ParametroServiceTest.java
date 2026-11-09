package com.stockiq.parametro.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stockiq.common.exception.BadRequestException;
import com.stockiq.common.exception.NotFoundException;
import com.stockiq.parametro.model.Parametro;
import com.stockiq.parametro.repository.ParametroRepository;

@ExtendWith(MockitoExtension.class)
class ParametroServiceTest {

    @Mock
    private ParametroRepository repositorio;

    private ParametroService servicio;

    @BeforeEach
    void setUp() {
        servicio = new ParametroService(repositorio);
    }

    private static Parametro parametro(String clave, String valor) {
        return Parametro.builder().clave(clave).valor(valor).descripcion("desc").build();
    }

    private void parametrosPorDefecto() {
        when(repositorio.findAll()).thenReturn(List.of(
                parametro(ParametroService.VENTANA_CONSUMO_DIAS, "30"),
                parametro(ParametroService.DIAS_COBERTURA, "30"),
                parametro(ParametroService.PERIODO_ANALISIS_DIAS, "90"),
                parametro(ParametroService.CORTE_ABC_A, "80"),
                parametro(ParametroService.CORTE_ABC_B, "95")));
    }

    // ------------------------------------------------------------------ días (RN-37)

    @Test
    void actualizar_diasValidos_guardaElValorNormalizado() {
        Parametro ventana = parametro(ParametroService.VENTANA_CONSUMO_DIAS, "30");
        when(repositorio.findById(ParametroService.VENTANA_CONSUMO_DIAS)).thenReturn(Optional.of(ventana));

        servicio.actualizar(ParametroService.VENTANA_CONSUMO_DIAS, " 45 ");

        assertThat(ventana.getValor()).isEqualTo("45");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "2,5", "abc", "3651"})
    void actualizar_diasInvalidos_responde400_RN37(String valor) {
        when(repositorio.findById(ParametroService.DIAS_COBERTURA))
                .thenReturn(Optional.of(parametro(ParametroService.DIAS_COBERTURA, "30")));

        assertThatThrownBy(() -> servicio.actualizar(ParametroService.DIAS_COBERTURA, valor))
                .isInstanceOf(BadRequestException.class)
                .extracting("codigo").isEqualTo("PARAMETRO_INVALIDO");
    }

    // ------------------------------------------------------------------ cortes ABC (RN-37)

    @Test
    void actualizar_corteConComa_loNormaliza() {
        Parametro corteA = parametro(ParametroService.CORTE_ABC_A, "80");
        when(repositorio.findById(ParametroService.CORTE_ABC_A)).thenReturn(Optional.of(corteA));
        parametrosPorDefecto();

        servicio.actualizar(ParametroService.CORTE_ABC_A, "75,5");

        assertThat(corteA.getValor()).isEqualTo("75.5");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "100", "-1", "80.123", "x"})
    void actualizar_corteFueraDeRango_responde400_RN37(String valor) {
        when(repositorio.findById(ParametroService.CORTE_ABC_B))
                .thenReturn(Optional.of(parametro(ParametroService.CORTE_ABC_B, "95")));

        assertThatThrownBy(() -> servicio.actualizar(ParametroService.CORTE_ABC_B, valor))
                .isInstanceOf(BadRequestException.class)
                .extracting("codigo").isEqualTo("PARAMETRO_INVALIDO");
    }

    @Test
    void actualizar_corteAMayorOIgualQueB_responde400_RN37() {
        when(repositorio.findById(ParametroService.CORTE_ABC_A))
                .thenReturn(Optional.of(parametro(ParametroService.CORTE_ABC_A, "80")));
        parametrosPorDefecto();

        assertThatThrownBy(() -> servicio.actualizar(ParametroService.CORTE_ABC_A, "95"))
                .isInstanceOf(BadRequestException.class)
                .extracting("codigo").isEqualTo("CORTES_ABC_INVALIDOS");
    }

    @Test
    void actualizar_claveInexistente_responde404() {
        when(repositorio.findById("no_existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar("no_existe", "1")).isInstanceOf(NotFoundException.class);
    }

    // ------------------------------------------------------------------ configuración

    @Test
    void configuracion_conValoresFaltantesOInvalidos_usaLosPorDefecto() {
        when(repositorio.findAll()).thenReturn(List.of(
                parametro(ParametroService.VENTANA_CONSUMO_DIAS, "15"),
                parametro(ParametroService.CORTE_ABC_A, "no es un número")));

        ParametrosAnalitica p = servicio.configuracion();

        assertThat(p.ventanaConsumoDias()).isEqualTo(15);
        assertThat(p.diasCobertura()).isEqualTo(30);
        assertThat(p.periodoAnalisisDias()).isEqualTo(90);
        assertThat(p.corteAbcA()).isEqualByComparingTo(new BigDecimal("80"));
        assertThat(p.corteAbcB()).isEqualByComparingTo(new BigDecimal("95"));
    }
}
