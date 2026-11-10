package br.com.techalimenta.rotasolidaria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {
    /** Relógio injetável: permite testar expiração sem esperar o tempo passar. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
