package com.stockiq;

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
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    static {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            MYSQL.start();
        }
    }
}
