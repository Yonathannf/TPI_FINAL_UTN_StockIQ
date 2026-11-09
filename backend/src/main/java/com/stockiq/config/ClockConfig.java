package com.stockiq.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicación, en UTC (D-16). La capa analítica toma de acá la fecha de referencia en lugar de
 * usar NOW() en las consultas; así los tests pueden fijar la fecha sin depender del día en que se corren.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
