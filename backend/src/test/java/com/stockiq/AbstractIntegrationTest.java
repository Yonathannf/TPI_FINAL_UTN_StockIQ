package com.stockiq;

import java.util.Map;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de los tests de integración: aplicación completa contra un MySQL 8 real.
 *
 * El contenedor es único para toda la ejecución (se levanta una vez y Testcontainers lo elimina al
 * terminar la JVM), así todas las clases comparten el mismo contexto de Spring. Si no hay Docker, las
 * clases de integración se omiten en lugar de fallar.
 *
 * Los datos de MySQL viven en memoria (tmpfs) para que la inicialización sea rápida aun en máquinas con
 * disco lento, y los plazos de arranque son holgados para no fallar por una demora puntual.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withStartupTimeoutSeconds(300)
            .withConnectTimeoutSeconds(300);

    static {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            MYSQL.start();
        }
    }
}