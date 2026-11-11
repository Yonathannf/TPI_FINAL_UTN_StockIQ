package com.stockiq.deposito.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import com.stockiq.common.exception.ConflictException;
import com.stockiq.common.exception.NotFoundException;
import com.stockiq.deposito.dto.DepositoRequest;
import com.stockiq.deposito.dto.DepositoResponse;
import com.stockiq.deposito.mapper.DepositoMapper;
import com.stockiq.deposito.model.Deposito;
import com.stockiq.deposito.repository.DepositoRepository;
import com.stockiq.stock.repository.StockConsultaRepository;

@ExtendWith(MockitoExtension.class)
class DepositoServiceTest {

    private static final Sort ORDEN = Sort.by("nombre");

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

    // ---------- RN-38: baja de un depósito ----------

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
    void desactivarUnDepositoInexistenteDevuelveNoEncontrado() {
        when(depositoRepository.bloquearPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desactivar(99L))
                .isInstanceOf(NotFoundException.class)
                .extracting("codigo").isEqualTo("RECURSO_NO_ENCONTRADO");
    }

    // ---------- Edición ----------

    @Test
    void rechazaNombreDuplicadoAlEditar() {
        when(depositoRepository.findById(1L)).thenReturn(Optional.of(deposito(1L, true)));
        when(depositoRepository.existsByNombreIgnoreCaseAndIdNot("Central", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.editar(1L, new DepositoRequest("Central", null)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("NOMBRE_DUPLICADO");
    }

    @Test
    void editaElNombreYLaUbicacionSinEspacios() {
        Deposito d = deposito(1L, true);
        when(depositoRepository.findById(1L)).thenReturn(Optional.of(d));
        when(depositoRepository.existsByNombreIgnoreCaseAndIdNot("Sur", 1L)).thenReturn(false);

        DepositoResponse respuesta = service.editar(1L, new DepositoRequest("  Sur  ", "  Av. Siempreviva 123 "));

        assertThat(respuesta.nombre()).isEqualTo("Sur");
        assertThat(respuesta.ubicacion()).isEqualTo("Av. Siempreviva 123");
        assertThat(d.getNombre()).isEqualTo("Sur");
    }

    @Test
    void editarUnDepositoInexistenteDevuelveNoEncontrado() {
        when(depositoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editar(99L, new DepositoRequest("Sur", null)))
                .isInstanceOf(NotFoundException.class)
                .extracting("codigo").isEqualTo("RECURSO_NO_ENCONTRADO");
    }

    // ---------- Alta ----------

    @Test
    void creaUnDepositoActivoConElNombreSinEspacios() {
        when(depositoRepository.existsByNombreIgnoreCase("Norte")).thenReturn(false);
        when(depositoRepository.save(any(Deposito.class))).thenAnswer(inv -> inv.getArgument(0));

        DepositoResponse respuesta = service.crear(new DepositoRequest("  Norte  ", "   "));

        assertThat(respuesta.nombre()).isEqualTo("Norte");
        assertThat(respuesta.ubicacion()).isNull();
        assertThat(respuesta.activo()).isTrue();
    }

    @Test
    void rechazaNombreDuplicadoAlCrear() {
        when(depositoRepository.existsByNombreIgnoreCase("Central")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new DepositoRequest("Central", null)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("NOMBRE_DUPLICADO");
        verify(depositoRepository, never()).save(any(Deposito.class));
    }

    // ---------- Consulta y reactivación ----------

    @Test
    void listarSinFiltroDevuelveTodosOrdenadosPorNombre() {
        when(depositoRepository.findAll(ORDEN)).thenReturn(List.of(deposito(1L, true), deposito(2L, false)));

        List<DepositoResponse> respuesta = service.listar(null);

        assertThat(respuesta).extracting(DepositoResponse::id).containsExactly(1L, 2L);
        verify(depositoRepository, never()).findByActivo(anyBoolean(), any(Sort.class));
    }

    @Test
    void listarConFiltroConsultaPorEstado() {
        when(depositoRepository.findByActivo(true, ORDEN)).thenReturn(List.of(deposito(1L, true)));

        List<DepositoResponse> respuesta = service.listar(true);

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.get(0).activo()).isTrue();
    }

    @Test
    void obtenerDevuelveElDeposito() {
        when(depositoRepository.findById(1L)).thenReturn(Optional.of(deposito(1L, true)));

        assertThat(service.obtener(1L).id()).isEqualTo(1L);
    }

    @Test
    void obtenerUnDepositoInexistenteDevuelveNoEncontrado() {
        when(depositoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L))
                .isInstanceOf(NotFoundException.class)
                .extracting("codigo").isEqualTo("RECURSO_NO_ENCONTRADO");
    }

    @Test
    void activaUnDepositoInactivo() {
        Deposito d = deposito(1L, false);
        when(depositoRepository.findById(1L)).thenReturn(Optional.of(d));

        assertThat(service.activar(1L).activo()).isTrue();
        assertThat(d.isActivo()).isTrue();
    }

    @Test
    void activarUnDepositoInexistenteDevuelveNoEncontrado() {
        when(depositoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activar(99L))
                .isInstanceOf(NotFoundException.class)
                .extracting("codigo").isEqualTo("RECURSO_NO_ENCONTRADO");
    }
}
