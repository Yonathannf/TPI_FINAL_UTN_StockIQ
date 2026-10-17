package com.stockiq;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Comprueba que Flyway aplica V1 y V2 sin errores sobre un MySQL real y que arranca el seeder. */
class MigracionesFlywayTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void v1CreaLosParametrosPorDefecto() {
        Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM parametro", Integer.class);
        assertThat(cantidad).isEqualTo(5);
    }

    @Test
    void v2AgregaLasColumnasDelCierreDeAnalisis() {
        assertThat(existeColumna("categoria", "activo")).isTrue();
        assertThat(existeColumna("producto", "fraccionable")).isTrue();
    }

    @Test
    void lasVistasDeStockExisten() {
        assertThat(jdbc.queryForList("SELECT * FROM v_stock_deposito")).isEmpty();
        assertThat(jdbc.queryForList("SELECT * FROM v_stock_transito")).isEmpty();
    }

    @Test
    void elSeederCreaElEncargadoInicial() {
        Integer cantidad = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE username = 'test-admin' AND rol = 'ENCARGADO' AND activo = TRUE",
                Integer.class);
        assertThat(cantidad).isEqualTo(1);
    }

    private boolean existeColumna(String tabla, String columna) {
        Integer cantidad = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, tabla, columna);
        return cantidad != null && cantidad > 0;
    }
}
