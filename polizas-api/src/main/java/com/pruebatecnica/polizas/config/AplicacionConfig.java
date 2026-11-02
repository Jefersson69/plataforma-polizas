package com.pruebatecnica.polizas.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConfigurationPropertiesScan("com.pruebatecnica.polizas")
@EnableScheduling
class AplicacionConfig {

    @Bean
    Clock reloj() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }
}
