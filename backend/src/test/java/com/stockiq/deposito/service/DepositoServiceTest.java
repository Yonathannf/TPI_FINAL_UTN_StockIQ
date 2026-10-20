package com.stockiq.deposito.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stockiq.common.exception.ConflictException;
import com.stockiq.deposito.dto.DepositoRequest;
import com.stockiq.deposito.mapper.DepositoMapper;
import com.stockiq.deposito.model.Deposito;
import com.stockiq.deposito.repository.DepositoRepository;
import com.stockiq.stock.repository.StockConsultaRepository;

@ExtendWith(MockitoExtension.class)
class DepositoServiceTest {

    @Mock
    private DepositoRepository depositoRepository;
    @Mock
    private StockConsultaRepository stockConsulta;

    private DepositoService service;

    @BeforeEach
    void setUp() {
        service = new DepositoService(depositoRepository, stockConsulta, new DepositoMapper());
    }

    private static Deposito deposito(Long id, boolean activo) {
        Deposito d = new Deposito();
        d.setId(id);
        d.setNombre("Depósito " + id);
        d.setActivo(activo);
        return d;
    }

    @Test
    void noPermiteDesactivarConStock() {
        Deposito d = deposito(1L, true);
        when(depositoRepository.bloquearPorId(1L)).thenReturn(Optional.of(d));
        when(stockConsulta.depositoTieneStock(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.desactivar(1L))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("DEPOSITO_CON_STOCK");
        assertThat(d.isActivo()).isTrue();
    }

    @Test
    void noPermiteDesactivarConTransferenciasPendientes() {
        Deposito d = deposito(1L, true);
        when(depositoRepository.bloquearPorId(1L)).thenReturn(Optional.of(d));
        when(stockConsulta.depositoTieneStock(1L)).thenReturn(false);
        when(stockConsulta.depositoTieneTransferenciasPendientes(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.desactivar(1L))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("DEPOSITO_CON_TRANSFERENCIAS");
    }

    @Test
    void desactivaUnDepositoVacio() {
        Deposito d = deposito(1L, true);
        when(depositoRepository.bloquearPorId(1L)).thenReturn(Optional.of(d));
        when(stockConsulta.depositoTieneStock(1L)).thenReturn(false);
        when(stockConsulta.depositoTieneTransferenciasPendientes(1L)).thenReturn(false);

        assertThat(service.desactivar(1L).activo()).isFalse();
    }

    @Test
    void desactivarUnDepositoYaInactivoNoConsultaElStock() {
        when(depositoRepository.bloquearPorId(1L)).thenReturn(Optional.of(deposito(1L, false)));

        service.desactivar(1L);

        verify(stockConsulta, never()).depositoTieneStock(anyLong());
    }

    @Test
    void rechazaNombreDuplicadoAlEditar() {
        when(depositoRepository.findById(1L)).thenReturn(Optional.of(deposito(1L, true)));
        when(depositoRepository.existsByNombreIgnoreCaseAndIdNot("Central", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.editar(1L, new DepositoRequest("Central", null)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("NOMBRE_DUPLICADO");
    }
}
