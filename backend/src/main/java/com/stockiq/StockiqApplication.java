package com.stockiq;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Sin UserDetailsService: la autenticación la resuelve el filtro JWT, así Spring no genera una clave aleatoria.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class StockiqApplication {

    public static void main(String[] args) {
        // D-16 / RN-41: todas las fechas se manejan en UTC sin importar dónde corra el backend.
        // La conversión a hora de Argentina se hace solo al mostrar, filtrar o agrupar por día.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(StockiqApplication.class, args);
    }
}
